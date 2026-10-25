# usuarios

Plantilla pública y reutilizable de gestión de usuarios con **Spring Boot 4** y **Spring Security 7**:
registro con consentimiento, verificación de email, inicio y cierre de sesión con bloqueo,
recuperación y cambio de contraseña. Sirve de base para proyectos que necesiten la autenticación,
la autorización y la administración de usuarios ya resueltas, con OWASP ASVS nivel 2 como
referencia de seguridad.

Se desarrolla con [Spec Kit](https://github.com/github/spec-kit): los principios del proyecto están
en la [constitución](.specify/memory/constitution.md) y cada funcionalidad en `specs/`.

## Requisitos

- Java 25
- PostgreSQL 18, con las bases de datos `usuarios` (desarrollo) y `usuarios_test` (tests)
- Mailpit en desarrollo, para capturar los emails
- No hace falta Maven: el repositorio incluye el wrapper `mvnw`

La guía de arranque y validación está en
[specs/001-registro-acceso/quickstart.md](specs/001-registro-acceso/quickstart.md).

```bash
./mvnw verify
./mvnw verify -Prendimiento
./mvnw -pl usuarios-web -am spring-boot:run -Dspring-boot.run.profiles=local
```

La prueba de rendimiento (100 sesiones durante un minuto) queda fuera del build normal.

## Estructura

| Módulo | Contenido |
|---|---|
| `usuarios-core` (JAR) | Dominio, servicios, seguridad, migraciones Flyway y emails. Sin controladores ni vistas |
| `usuarios-web` (WAR) | Controladores, vistas Thymeleaf y arranque. Se despliega en Tomcat 11 o con `java -jar` |

## Cómo adaptarla en un proyecto derivado

La idea es **no modificar `usuarios-core`**, para poder traer después sus mejoras y parches sin
conflictos. Todo lo siguiente se hace desde `usuarios-web` o desde la configuración.

| Qué | Cómo |
|---|---|
| Plazos, límites y política de contraseñas | Propiedades `usuarios.*` (o variables de entorno `USUARIOS_*`). Lista completa en [contracts/configuracion.md](specs/001-registro-acceso/contracts/configuracion.md). Desactivar el auto-registro: `usuarios.registro.habilitado=false` |
| Emails | Crear en `usuarios-web/src/main/resources/personalizado/email/` un fichero con el mismo nombre que el del core (`verificacion.html`, `verificacion.txt`, `fragmentos.html`, …). Si existe, se usa en lugar del original |
| Textos de los emails y de los errores de contraseña | Repetir la clave en `usuarios-web/src/main/resources/messages.properties`; prevalece sobre `usuarios/mensajes.properties` del core |
| Vistas y textos de la web | Están en `usuarios-web`: se editan directamente |
| Textos legales y su versión | Vistas `templates/legal/*.html` y propiedades `usuarios.legal.condiciones.version` y `usuarios.legal.privacidad.version`. Cada aceptación guarda la versión vigente |
| Envío de email (otro proveedor, una cola…) | Declarar un bean propio que implemente `NotificadorEmail`; el SMTP por defecto deja de registrarse |
| Datos propios del usuario | Tablas propias relacionadas por `usuario_id`, con migraciones Flyway a partir de `V1000` (el core usa `V1`–`V999`) |
| Varias instancias | Añadir Spring Session JDBC (`spring-boot-starter-session-jdbc`) y su configuración: las sesiones pasan a la base de datos y el registro de sesiones las usa sin cambiar código. La limitación de peticiones por origen sigue siendo por instancia; un límite global corresponde al proxy inverso |

Si la aplicación va detrás de un proxy inverso, hay que declarar su IP en
`usuarios.limite-peticiones.proxies-confianza` para que la limitación use la IP real del cliente
(`X-Forwarded-For`).

## Textos legales

Las condiciones de uso y la política de privacidad incluidas son **textos de ejemplo**. Cada
proyecto derivado debe sustituirlos por los suyos, revisados por un profesional competente.

## Hoja de ruta

| Feature | Contenido |
|---|---|
| 001 | Registro y acceso (en curso) |
| 002 | Roles, administración de usuarios y menús por rol |
| 003 | Auditoría de eventos de seguridad (principio VIII) |
| 004 | Exportación y supresión de datos a petición del usuario (principio IV) |
| 005 | MFA (segundo factor) |

## Créditos

- Lista de contraseñas prohibidas: `10k-most-common.txt` de
  [SecLists](https://github.com/danielmiessler/SecLists) (Daniel Miessler y colaboradores),
  licencia MIT.
