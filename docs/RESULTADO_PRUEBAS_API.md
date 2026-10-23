# Resultado de pruebas de la API

**Fecha:** 2026-10-23
**Comando:** `.\gradlew.bat test`

## Pruebas automatizadas y HTTP local

| Suite | Pruebas | Correctas | Fallidas | Omitidas |
|---|---:|---:|---:|---:|
| `GlobalExceptionHandlerTest` | 4 | 4 | 0 | 0 |
| `ApiNegativeHttpTest` (HTTP local) | 12 | 12 | 0 | 0 |
| `AutenticacionInterceptorTest` | 3 | 3 | 0 | 0 |
| `ControlAccesoTest` | 3 | 3 | 0 | 0 |
| `ClienteServiceImplTest` | 22 | 22 | 0 | 0 |
| `CuentaServiceImplTest` | 4 | 4 | 0 | 0 |
| `LoginServiceImplTest` | 5 | 5 | 0 | 0 |
| `ValidacionesTest` | 55 | 55 | 0 | 0 |
| **Total** | **108** | **108** | **0** | **0** |

**Resultado:** `BUILD SUCCESSFUL`.

`ApiNegativeHttpTest` inició un servidor web embebido en un puerto local aleatorio y envió solicitudes HTTP reales. Las pruebas usan los controladores, validación, manejador global y autenticación de la aplicación; los servicios de negocio se simulan para no requerir persistencia en estas solicitudes negativas.

La configuración de desarrollo apunta a una base PostgreSQL remota. Para evitar conectarse a ella o aplicar migraciones a datos ajenos, no se inició el contexto de producción. La configuración de prueba excluye datasource/JPA/Flyway y no realiza llamadas a servicios externos.

Gradle mostró una advertencia de funciones obsoletas que podrían ser incompatibles con Gradle 10; no afectó esta ejecución.

### Casos HTTP negativos ejecutados

| Caso | Esperado | Resultado |
|---|---:|---|
| Alta de cliente con campos requeridos/formato inválidos; comprueba varios mensajes en un campo | 400 | Correcto |
| Consulta de clientes con id negativo | 400 | Correcto |
| Consulta de clientes con campo no permitido | 400 | Correcto |
| Consulta de cuentas con número corto | 400 | Correcto |
| Consulta de saldo sin número de cuenta | 400 | Correcto |
| Consulta de cuentas con fecha inexistente | 400 | Correcto |
| Consulta de cuentas con estatus fuera del catálogo | 400 | Correcto |
| Login con correo y contraseña inválidos | 400 | Correcto |
| Consulta de nacionalidad con valor vacío | 400 | Correcto |
| JSON malformado | 400 | Correcto |
| Endpoint protegido sin token | 401 | Correcto |
| Endpoint protegido con token inválido | 401 | Correcto |

## Pruebas REST Client preparadas

El archivo [onboarding-api.rest](../requests/onboarding-api.rest) contiene solicitudes REST Client, incluidas solicitudes positivas y negativas, con aserciones de estado HTTP para los endpoints disponibles:

| Endpoint | Resultado esperado |
|---|---:|
| `POST /clientes` | 201 |
| `POST /login` | 200 |
| `POST /clientes/consulta` | 200 |
| `PUT /clientes` | 200 |
| `DELETE /clientes` | 200 |
| `GET /cuentas/activas` | 200 |
| `POST /cuentas/consulta` | 200 |
| `POST /cuentas/saldo` | 200 |
| `GET /nacionalidades` | 200 |
| `POST /nacionalidades/consulta` | 200 |
| `POST /personas` | 201 |
| `PUT /personasActualiza` | 200 |
| `PUT /personasElimina` | 200 |
| Validación negativa de `POST /clientes` | 400 |

Las solicitudes enlazan el token, id y número de cuenta devueltos por el login/alta. Para probarlas contra la aplicación completa, iniciar el servicio con una base de datos de desarrollo autorizada y usar **Send Request** desde VS Code en orden; la baja lógica del cliente está al final. Antes de repetir la creación, sustituir CURP, RFC y correo por datos que no existan en esa base.

**Alcance:** las 108 pruebas automatizadas se ejecutaron, incluidas las 12 pruebas HTTP negativas en servidor local. Las solicitudes completas del archivo `.rest` quedaron preparadas pero no se enviaron contra la base de datos; sus códigos son expectativas declaradas, no resultados de integración.
