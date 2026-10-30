# Pagos de servicios y recargas con GestoPago (sendTx)

## 1. Qué hace
Permite a un cliente autenticado pagar un servicio o una recarga del catálogo de GestoPago con el saldo
de su cuenta. Antes de mover dinero se valida la identidad (JWT + segundo factor TOTP), la cuenta, el
saldo y el producto; luego se retiene el saldo, se envía la transacción cifrada a
`POST /sistema/service/sendTx.do` y se resuelve el resultado. Si no se puede saber qué pasó, el pago
queda en conciliación con el dinero retenido y un job lo resuelve con `confirmTx.do`.

## 2. Flujo

```
POST /pagos  (Authorization: Bearer <jwt>, Idempotency-Key: <clave única>)
  1. Idempotency-Key ya usada?  -> misma solicitud: devuelve el pago existente (no recobra)
                                -> otros datos: 409 IDEMPOTENCIA_CONFLICTO
  2. Producto en catálogo (PostgreSQL), idServicio, tipoFront 1 o 2,
     recarga: monto = precio; servicio: monto dentro de límites
     y comisión = precio del catálogo; teléfono/referencia             -> 400 / 404
  3. Llave AES del proveedor configurada                               -> 503
  4. Código 2FA (TOTP) válido, no reutilizado, sin bloqueo             -> 401 / 403 / 423
  5. TX corta: SELECT ... FOR UPDATE de la cuenta, titular, cliente y
     cuenta activos, saldo suficiente, saldo -= monto + comisión, INSERT pago
     PENDIENTE (UNIQUE upc y cliente+clave)                            -> 403 / 409
  6. sendTx (fuera de la TX, timeout 62 s, token Bearer de GestoPago)
       01                       -> APLICADO                             200
       otro código              -> RECHAZADO + reembolso                422
       timeout / 5xx / 82 / 03 / XML ilegible:
         si ya pasaron 62 s desde el envío -> confirmTx (mismo payload)
            01                  -> APLICADO                             200
            70 (no aplicada)    -> RECHAZADO + reembolso                422
            cualquier otra cosa -> EN_CONCILIACION (saldo retenido)     202
         si no han pasado 62 s -> EN_CONCILIACION; confirmTx lo hace el job  202
       token rechazado dos veces / sin token -> RECHAZADO + reembolso   502

Job de conciliación (cada 5 min): PENDIENTE viejos y EN_CONCILIACION -> confirmTx
       01 -> APLICADO; 70 -> RECHAZADO + reembolso;
       sin respuesta -> intento++; al agotar intentos -> REVISION_MANUAL (saldo retenido)
       más de 24 h desde el envío -> REVISION_MANUAL sin llamar (confirmTx ya no responde)

GET /pagos/{id}  consulta el estatus de un pago propio (para los 202)
```

### Segundo factor (TOTP)
| Endpoint | Qué hace |
|---|---|
| `POST /2fa/registro` | Genera un secreto de 160 bits y devuelve `secreto` y `uriOtpauth` (para QR). Respuesta `Cache-Control: no-store`. Se puede regenerar mientras no esté activo. |
| `POST /2fa/activar` `{codigo}` | Activa el 2FA cuando el cliente demuestra que su app genera códigos correctos. |

Cada pago lleva `codigo2fa` en el cuerpo (como en el diagrama). Se eligió TOTP (RFC 6238, compatible con
Google/Microsoft Authenticator) en lugar de un código por correo o SMS porque no depende de otro proveedor
externo ni de credenciales de envío, y el código no viaja por un canal que se pueda interceptar.

## 3. Decisiones de diseño

**Retener primero, confirmar después (dos fases), sin bloquear durante la llamada externa.**
El saldo se descuenta en la misma transacción que crea el pago `PENDIENTE`, con la fila de la cuenta
bloqueada. La llamada a GestoPago ocurre después, ya sin bloqueo. Alternativas descartadas:

- *Descontar solo cuando GestoPago responde 01:* obliga a mantener el bloqueo de la cuenta hasta 62 s
  (o a arriesgar un sobregiro si se libera), y si la app se cae después de que el proveedor cobró, el
  cliente queda con un servicio pagado sin descuento.
- *Descontar y llamar en la misma transacción:* una transacción de base de datos abierta durante una
  llamada HTTP agota el pool y, si el proveedor cobró y la transacción hace rollback, se pierde el cargo.

Con la retención el dinero nunca se usa dos veces y siempre hay un registro (`pagos`) que explica dónde está.

**Un timeout no es un rechazo.** Timeout, error de red, 5xx, código 82 ("tiempo de espera alcanzado"),
código 03 ("el carrier no responde") o una respuesta ilegible se tratan como INDETERMINADO: no se
reembolsa ni se reintenta `sendTx`. Se consulta `confirmTx` con exactamente el mismo payload (mismo `upc`
y `horaLocal`), respetando la especificación: nunca antes de 62 s desde el envío ni después de 24 h.

**Solo el código 70 de confirmTx libera el dinero.** Es la respuesta documentada para "La transacción no
fue aplicada" (verificado contra el servicio con un UPC que nunca se envió). Cualquier otro código deja el
pago en conciliación: reembolsar con base en una suposición puede regalar el servicio.

**La comisión la paga el cliente.** En los pagos de servicio (`tipoFront` 2) el `precio` del catálogo es la
comisión de la transacción, no el importe: se retiene `monto + comisión` y ambos se guardan por separado.

**Un token por día.** PuntoRed entrega tokens válidos por 24 h y prohíbe pedir uno en cada petición. Se
reutiliza el guardado en `gestopago_tokens` y solo se renueva al vencer (23 h, con margen) o si el proveedor
lo rechaza (401/403); en ese caso se reintenta una vez, porque el rechazo garantiza que no se procesó.

**Validación contra PostgreSQL, no contra Redis.** Redis es la cache de lectura del catálogo; una regla
de dinero no debe depender de un dato que se podría alterar en la cache.

## 4. Concurrencia y duplicados

| Riesgo | Protección |
|---|---|
| Dos pagos simultáneos de la misma cuenta que juntos exceden el saldo | `SELECT ... FOR UPDATE` en `cuentas` y verificación del saldo bajo el bloqueo (timeout de bloqueo 5 s → 409 `OPERACION_EN_CURSO`). |
| Doble clic / reintento del cliente | `Idempotency-Key` obligatoria + `UNIQUE (cliente_id, clave_idempotencia)`. La segunda petición recibe el mismo pago. |
| Misma clave con otros datos | 409 `IDEMPOTENCIA_CONFLICTO`. |
| Reutilizar un código 2FA interceptado | Se guarda el último paso TOTP aceptado; un paso igual o anterior se rechaza. El registro 2FA se bloquea con `FOR UPDATE` para que dos peticiones no acepten el mismo código. |
| Fuerza bruta del 2FA | 5 fallos → bloqueo de 15 min (423). Los fallos se guardan aunque la petición falle (`noRollbackFor`). |
| Dos resultados para el mismo pago (respuesta + job) | Las transiciones bloquean la fila del pago y no tocan pagos en estado final: un pago aplicado no se puede reembolsar ni uno reembolsado volver a reembolsarse. |
| Mismo `upc` dos veces | `UNIQUE (upc)`; GestoPago también rechaza UPC repetidos. |
| Caída de la app entre retener y llamar | El pago queda `PENDIENTE`; el job lo toma después de 5 min y pregunta a `confirmTx`. |

`PagoTransaccionesConcurrenciaTest` lo comprueba con hilos reales: 12 pagos simultáneos de 100 con saldo
de 500 → exactamente 5 aplicados y saldo 0; la misma clave enviada 12 veces → un solo cargo.

## 5. Seguridad de credenciales y datos

- Token Bearer de GestoPago: lo obtiene `GestoPagoTokenService` con las credenciales del distribuidor
  (variables de entorno), se guarda en `gestopago_tokens` y solo se agrega al header de la llamada.
- Llave/IV AES del proveedor (`GESTOPAGO_TX_LLAVE_AES`, `GESTOPAGO_TX_IV_AES`) y clave de cifrado del
  2FA (`SEGUNDO_FACTOR_CLAVE_CIFRADO`): solo variables de entorno, sin valor por defecto. Sin ellas la
  funcionalidad responde 503 y no se retiene saldo.
- Secretos TOTP cifrados en reposo con AES-256-GCM (IV aleatorio por valor; detecta alteraciones).
- Logs: solo trazabilidad (`idPago`, `clienteId`, `upc`, código, duración). Nunca token, `signed`,
  payload, teléfono, referencia, código 2FA ni secretos. Los errores HTTP no incluyen mensajes internos
  del proveedor ni trazas.
- Bitácora (`gestopago_bitacora`): la especificación obliga a conservar cada petición y respuesta
  intercambiada. Se guarda el JSON sin cifrar, el estatus HTTP, el XML de respuesta y la duración; nunca
  el token ni `signed`. Se escribe fuera de la transacción del pago, así que se conserva aunque esta falle.

## 6. Respuestas HTTP

| Caso | HTTP | `error` |
|---|---|---|
| Pago aplicado | 200 | — |
| En verificación con el proveedor | 202 | — (`datos.estatus` = `EN_CONCILIACION`/`REVISION_MANUAL`) |
| Rechazado por el proveedor (monto devuelto) | 422 | — (`datos.estatus` = `RECHAZADO`) |
| Datos inválidos / monto manipulado / falta Idempotency-Key | 400 | `ERROR_VALIDACION` |
| Sin JWT o JWT inválido | 401 | `NO_AUTENTICADO` |
| Código 2FA inválido o reutilizado | 401 | `CODIGO_2FA_INVALIDO` |
| 2FA no activado / cuenta de otro cliente | 403 | `2FA_NO_CONFIGURADO` / `ACCESO_DENEGADO` |
| Producto inexistente | 404 | `PRODUCTO_NO_ENCONTRADO` |
| Saldo insuficiente / cuenta inactiva / operación en curso / clave reutilizada | 409 | `SALDO_INSUFICIENTE` / `CUENTA_INACTIVA` / `OPERACION_EN_CURSO` / `IDEMPOTENCIA_CONFLICTO` |
| 2FA bloqueado por intentos | 423 | `2FA_BLOQUEADO` |
| Token de GestoPago rechazado | 502 | `ERROR_AUTENTICACION_GESTOPAGO` |
| Llaves no configuradas | 503 | `SERVICIO_NO_CONFIGURADO` |

## 7. Contrato con GestoPago

Fuente: *PuntoRed API Technical Specification* (documentación oficial en Postman). Verificado contra el
servicio: la llave de prueba descifra el ejemplo cifrado de la especificación, y `confirmTx` de un UPC que
nunca se envió responde código 70.

- `sendTx.do` y `confirmTx.do` reciben `application/x-www-form-urlencoded` con `random` (timestamp) y
  `signed` = Base64(AES-256-CBC/PKCS5Padding(JSON)), con la llave e IV que entrega PuntoRed. Responden
  HTTP 200 con XML aunque haya error (`RESPONSE/MENSAJE/CODIGO|TEXTO`, o `AUTORIZACION/CODIGO_MENSAJE|MENSAJE`).
- JSON: `idProducto`, `idServicio`, `telefono`, `horaLocal` (`yyyyMMddHHmmss`), `upc` (3–15 caracteres,
  único), `unidad` (unidad comercial o sucursal, 3–25 caracteres) y, solo en pagos de servicio,
  `referencia` y `montoPago`.
- `tipoFront` 1 (recargas): el teléfono o la referencia van en `telefono`. `tipoFront` 2 (pagos):
  `telefono` fijo en `1111111111`. `idCatTipoServicio` 10 y 11 (canjeables): `telefono` = `#1111111111`.
  4 y 5 son consultas de referencia y 30/31/40 operaciones de efectivo: quedan excluidos.
- Códigos: 01 éxito, 02 saldo insuficiente del distribuidor, 06 transacción duplicada, 82 tiempo de
  espera alcanzado, 70 (confirmTx) no aplicada.
- Duplicados en el proveedor: recargas con el mismo UPC en 15 min; pagos de servicio con el mismo UPC o
  el mismo servicio y monto en 24 h.
- `getProductList` debe llamarse una vez al día y como máximo 3; si se excede, PuntoRed bloquea la IP.
  Por eso el catálogo se sirve desde PostgreSQL/Redis y la resincronización automática está apagada.

## 8. Configuración

| Variable | Uso |
|---|---|
| `GESTOPAGO_TX_LLAVE_AES`, `GESTOPAGO_TX_IV_AES` | Llave (16/24/32 bytes) e IV (16 bytes) en Base64 |
| `GESTOPAGO_TX_UNIDAD` | Unidad comercial o sucursal que se reporta en cada venta (`083`) |
| `GESTOPAGO_TX_READ_TIMEOUT_MS` | Timeout de lectura de sendTx/confirmTx (62000) |
| `GESTOPAGO_TX_CODIGOS_INDETERMINADOS` | Códigos de sendTx que se consultan con confirmTx (`03,82`) |
| `GESTOPAGO_TX_CODIGOS_NO_APLICADA` | Códigos de confirmTx que permiten reembolsar (`70`) |
| `PAGOS_MONTO_MINIMO`, `PAGOS_MONTO_MAXIMO` | Límites del monto en pagos de servicio |
| `PAGOS_CONCILIACION_*` | Intervalo, antigüedad mínima y máximo de intentos del job |
| `SEGUNDO_FACTOR_CLAVE_CIFRADO` | 32 bytes en Base64 (`openssl rand -base64 32`) |

## 9. Pruebas

- Unitarias: TOTP con los vectores del RFC 6238, cifrado de secretos, servicio 2FA (reuso, bloqueo),
  parser de respuestas, validador de catálogo y orquestador del pago (todos los caminos de la sección 2).
- Integración HTTP del cliente Feign contra un servidor local: formulario `random`/`signed` cifrado,
  Bearer, timeout real, 5xx, 401, 400, sin conexión.
- Integración de concurrencia con base H2 y bloqueos de fila.
- Integración HTTP de los controllers (estatus y cuerpos de error).
- Funcional: la aplicación real con PostgreSQL y un proveedor simulado cubrió alta, login, registro y
  activación de 2FA, saldo insuficiente, monto manipulado, pago exitoso, idempotencia, reuso de código,
  rechazo con reembolso, timeout resuelto con confirmTx, conciliación por el job y ausencia de datos
  sensibles en los logs.
