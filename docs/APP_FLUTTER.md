# App móvil Flutter – Plataforma de pagos GestoPago

Aplicación Flutter (Material 3) para Android, iOS y web. Consume el backend Spring de este
repositorio, que es quien habla con la API de PuntoRed/GestoPago. La app web del proyecto debe
consumir **los mismos endpoints**, así que un usuario entra con las mismas credenciales en ambas.

## 1. Arquitectura

```
App Flutter / Front web  ──HTTPS + JWT──▶  Backend Spring (este repo)  ──AES + JWT GestoPago──▶  PuntoRed
                                              │  PostgreSQL (clientes, cuentas, pagos, catálogo)
                                              └─ Redis (caché del catálogo y del límite de descargas)
```

Las credenciales de PuntoRed (distribuidor, dispositivo, llave AES) **solo viven en el backend**,
en variables de entorno. La app nunca las recibe; solo maneja el token de sesión del cliente.

Capas de `app_flutter/lib`:

| Capa | Carpeta | Responsabilidad |
|---|---|---|
| Núcleo | `core/` | Configuración (`API_BASE_URL`), cliente HTTP, errores, sesión, validaciones, formato |
| Datos | `data/models`, `data/repositories` | Modelos con (de)serialización JSON y repositorios: único lugar que conoce las rutas del backend |
| Funcionalidades | `features/` | Pantallas por módulo: `auth`, `catalogo`, `pago`, `historial`, `cuenta`, `home` |
| Compartido | `shared/widgets` | Componentes reutilizables: logotipo, estado del pago, comprobante, vistas de carga/error/vacío |
| App | `app/` | Tema, rutas (`go_router`) y protección de rutas según la sesión |

Estado: `provider` (`SessionController` como `ChangeNotifier` y repositorios inyectados).
Navegación: `go_router` con `StatefulShellRoute` (barra inferior en teléfono, riel lateral desde 840 px).

## 2. Servicios consumidos

| Módulo | Método y ruta | Uso en la app |
|---|---|---|
| Autenticación | `POST /login` | Inicia sesión; devuelve JWT, vigencia y número de cuenta |
| Registro | `POST /clientes` | Alta del cliente y apertura de su cuenta |
| Registro | `GET /nacionalidades` | Catálogo para el formulario de alta |
| Catálogo | `GET /productos` | Catálogo real de GestoPago (servido desde PostgreSQL/Redis) |
| Catálogo | `GET /productos/servicios/{id}/logo` | Logotipo del proveedor (PuntoRed `images/gestopago/servicios/{id}.png`) |
| Catálogo | `GET /productos/servicios/{id}/ayuda-referencia` | Imagen de ayuda de la referencia (PuntoRed `referencias/{id}.jpg`) |
| Referencias | `POST /pagos/referencias/verificar` | Verifica la referencia con `verifyReference.do` de PuntoRed |
| Pagos | `POST /pagos` (+ `Idempotency-Key`) | Procesa el pago con `sendTx.do` / `confirmTx.do` |
| Pagos | `GET /pagos?pagina=&tamano=` | Historial del cliente autenticado |
| Pagos | `GET /pagos/{id}` | Detalle y estado vigente de una operación propia |
| Cuenta | `POST /cuentas/saldo` | Saldo disponible |
| Seguridad | `POST /2fa/registro`, `POST /2fa/activar` | Alta del segundo factor (TOTP) requerido para pagar |

Los cuatro endpoints marcados como nuevos en el backend (verificar referencia, historial, logotipo y
ayuda de referencia) se agregaron para cubrir los requerimientos 2, 3 y 5. Los logotipos se sirven
desde el backend porque PuntoRed no envía encabezados CORS y el front web no podría descargarlos.

## 3. Cómo se cumple cada requerimiento

1. **Autenticación.** Login con correo y contraseña; registro con validación local y del backend.
   El JWT se guarda en almacenamiento seguro (Keychain/Keystore; en web, cifrado con WebCrypto) y se
   descarta al vencer o cuando el backend responde `NO_AUTENTICADO`. Cerrar sesión borra el token.
2. **Catálogo.** Proveedores agrupados con logotipo, categoría (`idCatTipoServicio`), descripción
   (`leyenda`) e identificador; búsqueda y filtro por categoría. Solo se muestran productos que se
   pueden pagar (`tipoFront` 1 y 2).
3. **Referencias.** La app valida el formato, pide capturarla dos veces y la verifica con el backend.
   El backend usa el producto de consulta (`tipoFront 4`) del mismo servicio, como pide PuntoRed.
   El botón para continuar **solo aparece con estado `VALIDA`**; si el dato se edita, se vuelve a pedir.
4. **Pago.** Resumen (servicio, referencia, importe, comisión, total, cuenta y saldo), código de
   seguridad, casilla de autorización y diálogo de confirmación. Resultado diferenciado:
   `APLICADO` = exitoso, `RECHAZADO` = rechazado con devolución, el resto = **sin confirmar**.
   Nunca se muestra como exitoso algo que el servicio no confirmó. La `Idempotency-Key` se conserva
   entre reintentos, así que repetir tras un corte de red no cobra dos veces.
5. **Historial.** Paginado, solo del cliente autenticado (el backend filtra por el token), con logotipo,
   fecha, referencia, importe, folio y estado; el detalle consulta el estado vigente.
6. **Plataformas.** Material 3, diseño adaptable (formulario en una o dos columnas, rejilla del catálogo
   según el ancho, barra o riel de navegación). Compila para Android, iOS y web.
7. **Requerimientos técnicos.** GET/POST con JSON, manejo de códigos HTTP (200/202/422/4xx/5xx), validación
   de formularios, excepciones tipadas (`ApiException`), capas separadas y estados de carga/éxito/error.

## 4. Configuración y ejecución

```bash
cd app_flutter
flutter pub get
flutter run -d chrome --dart-define=API_BASE_URL=http://localhost:8091
```

- `API_BASE_URL` se define al compilar; sin ella se usa `http://localhost:8091`
  (`http://10.0.2.2:8091` en el emulador de Android).
- En el backend, `CORS_ORIGENES` lista los orígenes web permitidos (por omisión `localhost` y
  `127.0.0.1` en cualquier puerto). La app móvil no necesita CORS.
- Para pagar, el cliente debe activar antes su código de seguridad en **Mi cuenta → Código de seguridad**.

## 5. Pruebas

- `flutter test`: modelos y reglas de captura, validaciones, cliente HTTP (sobre, errores, 422, sesión
  expirada, sin red), sesión (restaurar, vencer) y pantallas (login, bloqueo del pago hasta verificar).
- Backend (`./gradlew test`): verificación de referencias, historial, proxy de imágenes, CORS y endpoints HTTP.

Escenarios funcionales obligatorios (con el backend desplegado y el catálogo disponible):

| Escenario | Dónde |
|---|---|
| Inicio de sesión web y móvil con el mismo usuario | Pantalla de login |
| Catálogo con logotipos | Servicios |
| Referencia correcta / incorrecta | Datos del pago → Verificar referencia |
| Confirmación y resultado del pago | Confirma tu pago → Resultado |
| Historial y detalle desde ambas plataformas | Historial |

Los pagos deben probarse solo con el ambiente y las credenciales de prueba autorizados por PuntoRed.
