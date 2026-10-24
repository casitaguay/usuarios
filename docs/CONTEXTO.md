# Contexto del proyecto «usuarios»

Resumen de todo lo decidido y hecho hasta el **2026-10-25**, para que cualquier persona (o sesión
de Claude) pueda continuar sin el historial de la conversación original.

## 1. Qué es

Plantilla **pública y reutilizable** de gestión de usuarios con Spring Boot y Spring Security. Se
expondrá a internet y servirá de base para muchos proyectos que heredarán la autenticación, la
autorización y la administración de usuarios ya resueltas. Es un **proyecto personal**.

- Repositorio: <https://github.com/casitaguay/usuarios> (público, marcado como *template repository*)
- Metodología: **Spec Kit** (`specify → clarify → plan → tasks → analyze → implement`), con las
  skills en `.claude/skills/speckit-*`
- Idioma: todo en **español** (código de dominio, documentación, interfaz y mensajes de commit)

## 2. Estado actual

| Elemento | Estado |
|---|---|
| Constitución v1.0.0 | Hecha: [.specify/memory/constitution.md](../.specify/memory/constitution.md) |
| Feature 001 «Registro y acceso» | Spec, clarificación, plan, tareas y análisis hechos; **pendiente de implementar** |
| Rama de trabajo | `001-registro-acceso` (subida a GitHub, sin pull request todavía) |
| Código Java | Ninguno todavía |
| Siguiente paso | `/speckit-implement`, empezando por Setup y Foundational (T001–T038) |

Historial de commits:

| Commit | Rama | Contenido |
|---|---|---|
| `9641863` | `main` | Spec Kit inicializado y constitución v1.0.0 |
| `1e55636` | `001-registro-acceso` | Especificación de la feature 001 con 5 aclaraciones |
| `0d0a7c7` | `001-registro-acceso` | Plan técnico, research, modelo de datos, contratos y quickstart |
| `e4a784d` | `001-registro-acceso` | Tareas (82 en la primera versión) |
| `bda1284` | `001-registro-acceso` | Correcciones del análisis; tareas pasan a 86 |

## 3. Entorno local de desarrollo

| Herramienta | Estado |
|---|---|
| Sistema | Windows 10 Pro; terminal PowerShell (también Git Bash) |
| Java | 25.0.4 LTS instalado |
| Maven | 3.10.0 instalado (el proyecto usará Maven Wrapper `mvnw`) |
| PostgreSQL | 18.6 como servicio de Windows; `psql` en el PATH |
| Bases de datos | `usuarios` y `usuarios_test` **creadas**, propiedad del rol `usuarios` |
| Docker | **No instalado** (por eso no se usa Testcontainers todavía) |
| GitHub CLI (`gh`) | **No instalado**: los pull requests se crean desde la web |
| Mailpit | **Pendiente de descargar** (ejecutable de Windows; SMTP `localhost:1025`, web `localhost:8025`) |
| Git | Identidad configurada **solo en este repositorio** con el usuario `casitaguay` y su email *noreply* de GitHub. El push funciona con Git Credential Manager (credenciales ya guardadas) |

La contraseña del rol `usuarios` no está en el repositorio: va en
`usuarios-web/src/main/resources/application-local.yml`, que está excluido de git (se creará a
partir de `application-local.example.yml` al implementar).

## 4. Configuración de GitHub

- Repositorio público, marcado como **Template repository**.
- Activados: **Dependabot** (alertas y actualizaciones de seguridad), **Secret Protection** y
  **Push protection**.
- Ruleset `proteger-main` sobre la rama por defecto: pull request obligatorio, sin force push y sin
  borrado. **Pendiente** (tarea T085): exigir los checks de CI y CodeQL cuando se ejecuten por
  primera vez.
- CodeQL se configurará con un workflow propio (tarea T007), no desde los ajustes.
- Sin licencia por ahora (decisión del propietario; habrá que añadirla antes de que otros
  reutilicen el código).

## 5. Decisiones de arquitectura (y por qué)

| Tema | Decisión | Motivo |
|---|---|---|
| Reutilización | **Plantilla modular**: repositorio template con el código de usuarios aislado en `usuarios-core` | Lo más simple; deja abierta la opción de convertir el core en *starter* Maven |
| Frontend | **Thymeleaf + Bootstrap 5** renderizado en servidor; htmx solo si hace falta | Un solo WAR, seguridad de serie (CSRF, sesión), menús por rol triviales, sin Node. Se descartó React por duplicar trabajo; se puede añadir una API REST más adelante porque los servicios no dependen de la capa web |
| Autenticación | Sesión en cookie `HttpOnly` + CSRF | Lo más seguro y simple para web del mismo dominio |
| Despliegue | **WAR para Tomcat 11** (Spring Boot 4 lo exige) y también ejecutable con Tomcat embebido | Requisito del propietario; facilita una futura contenedorización |
| Base de datos | PostgreSQL con **Flyway** (migraciones SQL versionadas) | Esquema reproducible; prohibido `ddl-auto` fuera de tests |
| Tests | Contra **PostgreSQL real** (`usuarios_test`), sin H2 | H2 se comporta distinto que PostgreSQL con las migraciones |
| Multi-tenant | **No** | No hace falta y es muy caro de añadir después; se decidió explícitamente |
| Escala | Una instancia, 10.000 usuarios, 100 sesiones simultáneas | Aclaración 1; pasar a varias instancias solo requerirá Spring Session JDBC |
| Contraseñas | **OWASP ASVS**: 12 a 128 caracteres, sin reglas de composición, lista de prohibidas | El propietario propuso 10 caracteres con mayúscula y carácter especial; tras ver que contradecía la constitución (ASVS nivel 2) eligió ASVS. Mayúscula y carácter especial quedan como opción configurable desactivada |

Stack: Java 25, Spring Boot 4.1.2 (Spring Security 7), Spring Data JPA, Maven multi-módulo
(`usuarios-core` JAR y `usuarios-web` WAR), paquete `com.generico.usuarios`, Bouncy Castle (para
Argon2id), WebJar de Bootstrap, JUnit 5, MockMvc y Spring Security Test.

## 6. Constitución (resumen)

Ocho principios, con OWASP ASVS nivel 2 como referencia de seguridad:

1. **Seguridad por defecto** (no negociable): Argon2id, rutas privadas por defecto, CSRF, sin
   enumeración de usuarios, bloqueo y límite de intentos, enlaces de un solo uso con hash,
   cabeceras de seguridad, errores genéricos.
2. **Plantilla modular**: core aislado y extensible sin modificarlo.
3. **Simplicidad**: YAGNI; toda dependencia externa se justifica en el plan.
4. **Privacidad y RGPD**: minimización, consentimiento con versión, exportación y supresión.
5. **Tests**: toda la seguridad probada; cada regla de acceso con caso permitido y denegado.
6. **Portabilidad**: configuración por variables de entorno, sin estado en disco, logs a consola.
7. **Repositorio público**: nunca secretos; CodeQL, secret scanning y Dependabot activos.
8. **Auditoría**: eventos de seguridad registrados (se implementará en la feature 003).

Los textos legales y lo relativo al RGPD son mecanismos de la plantilla; su contenido real debe
revisarlo un profesional competente en cada proyecto derivado.

## 7. Feature 001: Registro y acceso de usuarios

Documentos en [specs/001-registro-acceso/](../specs/001-registro-acceso/):
`spec.md`, `research.md` (20 decisiones técnicas), `data-model.md`, `contracts/` (rutas web,
emails, configuración), `quickstart.md`, `tasks.md` y `checklists/requirements.md`.

**Historias**: (1) registro y verificación de email, P1; (2) inicio y cierre de sesión, P1;
(3) recuperación de contraseña, P2; (4) cambio de contraseña autenticado, P3. El **MVP** son la 1 y
la 2 juntas.

**Valores por defecto** (todos configurables con propiedades `usuarios.*`): enlace de verificación
24 h; enlace de recuperación 1 h; bloqueo de 15 min tras 5 fallos; 20 peticiones por minuto y
conexión (freno de 5 min); sesión de 30 min sin actividad y 12 h como máximo; sin "recordarme";
sesiones simultáneas sin límite; borrado de cuentas sin verificar a los 7 días.

**Decisiones técnicas más delicadas** (detalle en `research.md`):

- El bloqueo y la verificación de email se comprueban **después** de validar la contraseña, para no
  revelar qué cuentas existen por la respuesta ni por el tiempo (R5).
- Los emails se envían de forma **asíncrona tras el commit**, para que la respuesta sea idéntica
  exista o no la cuenta (R8).
- Los proyectos derivados sustituyen emails en `personalizado/email/` y textos en su
  `messages.properties`, sin tocar el core (R18).
- Limitación por conexión y política de contraseñas con código propio, sin librerías (R9, R10).

**Análisis (`/speckit-analyze`)**: encontró 2 hallazgos críticos (faltaban CodeQL y exigir CI en
verde para `main`), 1 alto (no estaba definido cómo personalizar plantillas), 5 medios y 4 bajos.
Todos corregidos en `bda1284`. Cobertura de requisitos por tareas: completa.

**Tareas**: 86 en 7 fases. Setup (T001–T009), Foundational (T010–T038), US1 (T039–T052),
US2 (T053–T065), US3 (T066–T072), US4 (T073–T078) y Polish (T079–T086).

## 8. Hoja de ruta

| Feature | Contenido |
|---|---|
| 001 | Registro y acceso (en curso) |
| 002 | Roles, administración de usuarios y menús por rol |
| 003 | Auditoría de eventos de seguridad (principio VIII) |
| 004 | Exportación y supresión de datos a petición del usuario (principio IV) |
| 005 | MFA (segundo factor) |

## 9. Cómo continuar

1. Descargar Mailpit y arrancarlo.
2. En la rama `001-registro-acceso`, ejecutar `/speckit-implement` **por fases**, parando al final
   de cada una para revisar: primero Setup y Foundational (T001–T038), después el MVP (US1 y US2).
3. Al crear `application-local.yml`, poner ahí la contraseña real del rol `usuarios`.
4. Abrir el pull request a `main` desde la web de GitHub; tras la primera ejecución de CI y CodeQL,
   completar T085 (checks obligatorios en el ruleset).

## 10. Forma de trabajar acordada

- Responder siempre en **español**, con tono profesional y directo; explicar el porqué de las
  decisiones y recomendar una opción cuando haya varias.
- Ante varias soluciones, proponer primero la más simple; no añadir dependencias ni servicios
  externos innecesarios.
- Confirmar antes de acciones difíciles de deshacer o que publican algo (push, PR, borrados).
- Commits con **Conventional Commits** en español; trabajo en ramas de feature y entrada a `main`
  solo por pull request.
- **Nunca** subir secretos ni pedir o manejar credenciales: el inicio de sesión en GitHub lo hace
  el propietario en su navegador (Git Credential Manager).
- Usar en los commits solo la identidad *noreply* de GitHub ya configurada en el repositorio.
