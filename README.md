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
./mvnw -pl usuarios-web spring-boot:run -Dspring-boot.run.profiles=local
```

## Estructura

| Módulo | Contenido |
|---|---|
| `usuarios-core` (JAR) | Dominio, servicios, seguridad, migraciones Flyway y emails. Sin controladores ni vistas |
| `usuarios-web` (WAR) | Controladores, vistas Thymeleaf y arranque. Se despliega en Tomcat 11 o con `java -jar` |

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
