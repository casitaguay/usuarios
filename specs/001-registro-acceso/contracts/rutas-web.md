# Contrato: rutas web

**Feature**: `001-registro-acceso`

Aplicación web renderizada en servidor. Todas las peticiones POST llevan token CSRF; sin él se
responde 403 con la página de error genérica. Las respuestas a formularios correctos usan el patrón
Post/Redirect/Get (302 a una página de resultado).

## Rutas públicas

| Método | Ruta | Campos del formulario | Resultado |
|---|---|---|---|
| GET | `/acceso` | — | Formulario de inicio de sesión |
| POST | `/acceso` | `email`, `contrasena` | Éxito → 302 a la página pedida o `/inicio`. Fallo (credenciales, bloqueo) → 302 a `/acceso?error` con mensaje genérico. Sin verificar con contraseña correcta → 302 a `/acceso?pendiente` con opción de reenvío |
| GET | `/registro` | — | Formulario de registro (404 si el registro está desactivado, FR-006) |
| POST | `/registro` | `nombre`, `email`, `contrasena`, `aceptaCondiciones`, `aceptaPrivacidad` | Datos válidos (exista o no el email) → 302 a `/registro/enviado`. Errores de validación → 200 con el formulario y los errores por campo (la contraseña nunca se devuelve) |
| GET | `/registro/enviado` | — | Mensaje genérico "si los datos son correctos, recibirás un email" |
| GET | `/verificacion?t={enlace}` | — | Válido → cuenta verificada, mensaje de éxito y enlace a `/acceso`. Ya verificada → mensaje informativo. Caducado, usado o manipulado → mensaje de enlace no válido con opción de reenvío |
| GET | `/verificacion/reenvio` | — | Formulario con `email` |
| POST | `/verificacion/reenvio` | `email` | Siempre 302 a `/verificacion/reenvio/enviado` con mensaje genérico |
| GET | `/recuperacion` | — | Formulario con `email` |
| POST | `/recuperacion` | `email` | Siempre 302 a `/recuperacion/enviado` con mensaje genérico |
| GET | `/recuperacion/nueva?t={enlace}` | — | Enlace válido → formulario de nueva contraseña (el enlace viaja en un campo oculto). No válido → mensaje y opción de pedir otro |
| POST | `/recuperacion/nueva` | `t`, `contrasena`, `confirmacion` | Éxito → 302 a `/acceso?recuperada`. Enlace no válido → mensaje de enlace no válido. Contraseña no válida → 200 con errores |
| GET | `/legal/condiciones`, `/legal/privacidad` | — | Textos legales vigentes |
| GET | `/error` | — | Página de error genérica |
| GET | `/actuator/health` | — | `{"status":"UP"}` sin detalles |

Recursos estáticos públicos: `/webjars/**`, `/css/**`, `/img/**`, `/favicon.ico`.

## Rutas privadas (requieren sesión)

| Método | Ruta | Campos del formulario | Resultado |
|---|---|---|---|
| GET | `/inicio` | — | Página de inicio con el nombre del usuario, enlace a cambio de contraseña y botón de cierre de sesión |
| GET | `/cuenta/contrasena` | — | Formulario de cambio de contraseña |
| POST | `/cuenta/contrasena` | `contrasenaActual`, `contrasena`, `confirmacion` | Éxito → 302 a `/inicio?contrasenaCambiada`. Errores → 200 con errores |
| POST | `/salir` | — | Invalida la sesión y 302 a `/acceso?salida` |

Cualquier otra ruta sin sesión → 302 a `/acceso` (y tras autenticarse, vuelta a la ruta pedida).

## Limitación por origen

`POST /acceso`, `POST /registro`, `POST /recuperacion` y `POST /verificacion/reenvio`: al superar
20 peticiones por minuto desde la misma IP, respuesta **429** con la página "Demasiados intentos,
espera unos minutos" durante 5 minutos (FR-014).

## Cabeceras en todas las respuestas

`Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; form-action 'self'`,
`Strict-Transport-Security` (solo en HTTPS), `X-Content-Type-Options: nosniff`,
`Referrer-Policy: same-origin`. Cookie de sesión `HttpOnly; Secure; SameSite=Lax`.
