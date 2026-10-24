# Contrato: configuración

**Feature**: `001-registro-acceso`

Propiedades propias bajo el prefijo `usuarios`, validadas al arrancar (FR-036). Los valores por
defecto son los de la spec. Los secretos solo por variables de entorno.

| Propiedad | Por defecto | Requisito |
|---|---|---|
| `usuarios.url-publica` | *(obligatoria)* | Enlaces de los emails |
| `usuarios.registro.habilitado` | `true` | FR-006 |
| `usuarios.contrasena.longitud-minima` | `12` | FR-023 |
| `usuarios.contrasena.longitud-maxima` | `128` | FR-023 |
| `usuarios.contrasena.exigir-mayuscula` | `false` | FR-024 |
| `usuarios.contrasena.exigir-caracter-especial` | `false` | FR-024 |
| `usuarios.verificacion.validez` | `24h` | FR-007 |
| `usuarios.verificacion.borrar-pendientes-tras` | `7d` | FR-010 |
| `usuarios.recuperacion.validez` | `1h` | FR-019 |
| `usuarios.bloqueo.intentos` | `5` | FR-012 |
| `usuarios.bloqueo.duracion` | `15m` | FR-012 |
| `usuarios.limite-peticiones.por-minuto` | `20` | FR-014 |
| `usuarios.limite-peticiones.freno` | `5m` | FR-014 |
| `usuarios.limite-peticiones.proxies-confianza` | *(vacío)* | R9 |
| `usuarios.sesion.inactividad` | `30m` | FR-015 |
| `usuarios.sesion.duracion-maxima` | `12h` | FR-015 |
| `usuarios.legal.condiciones.version` | `1.0-ejemplo` | FR-003, FR-004 |
| `usuarios.legal.privacidad.version` | `1.0-ejemplo` | FR-003, FR-004 |
| `usuarios.email.remitente` | *(obligatoria)* | Emails |
| `usuarios.limpieza.cron` | `0 0 3 * * *` | FR-010 |

Variables de entorno esperadas (nombres de Spring Boot estándar):

| Variable | Uso |
|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Base de datos |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Servidor SMTP |
| `USUARIOS_URL_PUBLICA`, `USUARIOS_EMAIL_REMITENTE` | Propiedades obligatorias |
