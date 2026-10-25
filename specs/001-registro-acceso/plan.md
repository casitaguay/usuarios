# Implementation Plan: Registro y acceso de usuarios

**Branch**: `001-registro-acceso` | **Date**: 2026-10-25 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-registro-acceso/spec.md`

## Summary

Primera feature de la plantilla: registro con consentimiento, verificación de email, inicio y
cierre de sesión con bloqueo temporal y limitación por origen, recuperación y cambio de contraseña,
sin revelar nunca si una cuenta existe. Se implementa como aplicación web renderizada en servidor
con Spring Boot 4.1 y Spring Security 7, en dos módulos Maven (`usuarios-core` y `usuarios-web`),
con PostgreSQL y Flyway. Las piezas clave del enfoque técnico ([research.md](research.md)) son:
comprobación de bloqueo y verificación **después** de validar la contraseña (R5), enlaces
aleatorios guardados como hash con consumo atómico (R7), envío de emails asíncrono tras el commit
(R8) y limitación por origen con un filtro propio en memoria (R9). Esta feature también deja lista
la base del repositorio: estructura Maven, CI, Dependabot, `.gitignore` y `.gitattributes`.

## Technical Context

**Language/Version**: Java 25 LTS

**Primary Dependencies**: Spring Boot 4.1.x (Spring Framework 7, Spring Security 7, Spring Data
JPA, Thymeleaf, Bean Validation, Java Mail, Actuator), Flyway, Bouncy Castle (requerido por Argon2),
WebJar de Bootstrap 5

**Storage**: PostgreSQL 18 (local en desarrollo y tests); esquema con Flyway

**Testing**: JUnit 5, Spring Boot Test, MockMvc, Spring Security Test; integración contra la base
de datos local `usuarios_test`; reloj inyectable para tiempos

**Target Platform**: servidor Linux o Windows con Tomcat 11 (WAR) o JVM 25 con Tomcat embebido

**Project Type**: aplicación web renderizada en servidor (monolito modular, plantilla reutilizable)

**Performance Goals**: 95% de páginas en menos de 2 s con 100 sesiones simultáneas (SC-009)

**Constraints**: una instancia; sin servicios externos salvo SMTP; sin CDN; diferencia media de
tiempo de respuesta < 50 ms entre emails con y sin cuenta (SC-003); WCAG 2.1 AA (SC-008)

**Scale/Scope**: hasta 10.000 usuarios registrados y 100 sesiones simultáneas; 14 rutas web y
5 plantillas de email

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo se cumple | Estado |
|---|---|---|
| I. Seguridad por defecto | Rutas privadas por defecto; Argon2id con `DelegatingPasswordEncoder`; cookie `HttpOnly`/`Secure`/`SameSite`; CSRF; cambio de id de sesión; sin enumeración (R5, R8); bloqueo y limitación por origen (R5, R9); enlaces de un uso con hash (R7); CSP, HSTS y demás cabeceras (R11); errores genéricos | ✅ |
| II. Plantilla modular | `usuarios-core` sin controladores ni vistas; extensión por configuración, plantillas de email y textos sustituibles sin tocar el core (R18), puerto `NotificadorEmail` y rango de migraciones reservado (data-model) | ✅ |
| III. Simplicidad | Sin librerías para limitación de peticiones ni contraseñas (R9, R10); sesión en memoria (R6). Única dependencia ajena a Spring: Bouncy Castle, justificada en R4; WebJar de Bootstrap justificado en R12 | ✅ |
| IV. Privacidad y RGPD | Consentimiento con versión y fecha (R13); borrado de cuentas sin verificar (R14); logs sin datos sensibles; sin servicios externos (R10). Exportación y supresión: aplazadas a la feature 004, registrada en la hoja de ruta del README | ✅ |
| V. Tests | Tests de toda la seguridad y de cada regla de acceso, permitida y denegada; integración contra PostgreSQL real (R16); el build falla si fallan tests | ✅ |
| VI. Portabilidad | Configuración por variables de entorno y `@ConfigurationProperties` (R15); sin estado en disco; logs a consola; `/actuator/health` mínimo; WAR + ejecutable (R3) | ✅ |
| VII. Repositorio público | `.gitignore` con `application-local.yml`; fichero de ejemplo con valores ficticios; CodeQL mediante workflow propio del repositorio (R19); secret scanning y push protection activados en los ajustes de GitHub; Dependabot con `.github/dependabot.yml` | ✅ |
| VIII. Auditoría | Tabla de auditoría aplazada a la feature 003, registrada en la hoja de ruta del README. Mientras tanto, eventos de seguridad en logs sin datos personales | ⚠️ Diferido, no incumplido |
| Flujo de desarrollo | Rama propia; PR a `main`; CI en GitHub Actions con compilación, tests y PostgreSQL; CodeQL en cada PR; checks de CI y CodeQL obligatorios en el ruleset de `main` (R19); Conventional Commits | ✅ |

**Resultado**: sin incumplimientos. El principio VIII se cubre en la feature de auditoría; esta
feature emite los eventos de seguridad de Spring Security para que esa feature solo tenga que
escucharlos. Re-evaluado tras la fase 1: sin cambios.

## Project Structure

### Documentation (this feature)

```text
specs/001-registro-acceso/
├── plan.md              # Este fichero
├── research.md          # Fase 0: decisiones técnicas
├── data-model.md        # Fase 1: tablas, estados y validaciones
├── quickstart.md        # Fase 1: guía de arranque y validación
├── contracts/
│   ├── rutas-web.md     # Rutas, formularios y respuestas
│   ├── emails.md        # Emails y puerto NotificadorEmail
│   └── configuracion.md # Propiedades usuarios.* y variables de entorno
└── tasks.md             # Fase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
pom.xml                                   # usuarios-parent (Java 25, Spring Boot 4.1.x)
mvnw, mvnw.cmd, .mvn/wrapper/
.gitignore, .gitattributes, README.md
.github/
├── workflows/ci.yml                      # Build y tests con PostgreSQL 18 como servicio
├── workflows/codeql.yml                  # Análisis de seguridad CodeQL (R19)
└── dependabot.yml                        # Maven y GitHub Actions

usuarios-core/                            # JAR: lógica y seguridad reutilizables, sin controladores ni vistas
├── pom.xml
└── src/
    ├── main/java/com/generico/usuarios/core/
    │   ├── config/          # UsuariosProperties, seguridad, sesiones, asincronía, reloj
    │   ├── dominio/         # Usuario, Consentimiento, EnlaceUnUso, enums de estado y finalidad
    │   ├── repositorio/     # Repositorios Spring Data JPA
    │   ├── servicio/        # Registro, verificación, acceso/bloqueo, recuperación, contraseña, limpieza
    │   ├── contrasena/      # Política de contraseñas y lista de prohibidas
    │   ├── enlace/          # Generación, hash y consumo de enlaces de un uso
    │   ├── seguridad/       # UserDetailsService, comprobaciones posteriores, limitador por origen, filtro de duración máxima
    │   └── email/           # Puerto NotificadorEmail, implementación SMTP, eventos y oyentes
    ├── main/resources/
    │   ├── db/migration/            # V1__usuarios.sql (core: V1–V999)
    │   ├── contrasenas-prohibidas.txt
    │   ├── usuarios/mensajes.properties   # Textos del core (emails), sustituibles (R18)
    │   └── templates/usuarios/email/      # Plantillas de email (HTML y texto)
    └── test/java/com/generico/usuarios/core/   # Tests unitarios y de integración del core

usuarios-web/                             # WAR: aplicación desplegable
├── pom.xml
└── src/
    ├── main/java/com/generico/usuarios/
    │   └── UsuariosApplication.java # Arranque + SpringBootServletInitializer (paquete raíz: escanea core y web)
    ├── main/java/com/generico/usuarios/web/
    │   ├── controlador/             # Acceso, registro, verificación, recuperación, cuenta, legal
    │   └── formulario/              # DTO de formularios con Bean Validation
    ├── main/resources/
    │   ├── application.yml
    │   ├── application-local.example.yml
    │   ├── messages.properties      # Textos en español; sus claves prevalecen sobre las del core
    │   ├── personalizado/email/     # (Opcional) emails propios del proyecto derivado (R18)
    │   ├── templates/               # Vistas Thymeleaf (layout, acceso, registro, ...)
    │   └── static/css/, static/img/
    └── test/java/com/generico/usuarios/web/    # Tests MockMvc de rutas, seguridad y cabeceras
```

**Structure Decision**: monolito modular con dos módulos Maven. `usuarios-core` contiene todo lo
reutilizable (incluida la configuración de Spring Security, para que un derivado la herede sin
copiarla) y no depende de controladores ni vistas; `usuarios-web` contiene solo la capa de
presentación y el arranque. Paquetes con la convención `com.generico.usuarios.<módulo>.<área>`.

## Complexity Tracking

No hay incumplimientos de la constitución que justificar.
