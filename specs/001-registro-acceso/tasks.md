---

description: "Lista de tareas de la feature 001: registro y acceso de usuarios"
---

# Tasks: Registro y acceso de usuarios

**Input**: Documentos de diseño en `/specs/001-registro-acceso/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: incluidos. La constitución (principio V) exige tests de toda la funcionalidad de
seguridad y, para cada regla de acceso, del caso permitido y del denegado. Dentro de cada historia,
los tests se escriben primero y deben fallar antes de implementar.

**Organization**: tareas agrupadas por historia de usuario para implementarlas y probarlas de
forma independiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (ficheros distintos, sin dependencias pendientes)
- **[Story]**: historia de usuario a la que pertenece (US1, US2, US3, US4)

## Path Conventions

Proyecto Maven multi-módulo (ver plan.md). Abreviaturas usadas en las tareas:

| Abreviatura | Ruta real |
|---|---|
| `core/` | `usuarios-core/src/main/java/com/generico/usuarios/core/` |
| `core-res/` | `usuarios-core/src/main/resources/` |
| `core-test/` | `usuarios-core/src/test/java/com/generico/usuarios/core/` |
| `web/` | `usuarios-web/src/main/java/com/generico/usuarios/web/` |
| `web-res/` | `usuarios-web/src/main/resources/` |
| `web-test/` | `usuarios-web/src/test/java/com/generico/usuarios/web/` |

Convenciones de código: nombres de dominio en español (constitución); los tests de integración
terminan en `IT` y usan la base de datos `usuarios_test`; los tests unitarios terminan en `Test`.
Todos los tiempos se obtienen del `Clock` inyectado, nunca de `Instant.now()` directo. Los logs
solo identifican al usuario por su id interno (FR-031).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: estructura del repositorio, build y análisis de seguridad

- [X] T001 Crear el POM padre `usuarios-parent` en `pom.xml` (groupId `com.generico.usuarios`, packaging `pom`, módulos `usuarios-core` y `usuarios-web`, padre `spring-boot-starter-parent` 4.1.2, `java.version` 25, codificación UTF-8, plugins surefire para `*Test` y failsafe para `*IT` en la fase `verify` excluyendo el paquete `rendimiento`, y perfil `rendimiento` que ejecuta solo los `*IT` de ese paquete)
- [X] T002 Generar Maven Wrapper con Maven 3.10.0 (`mvn wrapper:wrapper -Dmaven=3.10.0`), creando `mvnw`, `mvnw.cmd` y `.mvn/wrapper/maven-wrapper.properties`
- [X] T003 Crear `usuarios-core/pom.xml` (packaging `jar`) con los starters de Boot 4 de Spring Security, Spring Data JPA, Flyway (más `flyway-database-postgresql`), Thymeleaf, validation, mail y actuator; driver PostgreSQL; `org.bouncycastle:bcprov-jdk18on`; starters de test y `spring-security-test`; y `maven-jar-plugin` con objetivo `test-jar` para compartir utilidades de test con `usuarios-web`, excluyendo del test-jar `**/AplicacionPruebasCore*` (T036) para que `usuarios-web` no la escanee
- [X] T004 Crear `usuarios-web/pom.xml` (packaging `war`, `finalName` `usuarios-web`) con dependencia a `usuarios-core`, starter web MVC, starter de Tomcat en ámbito `provided`, `org.webjars:bootstrap` 5.x, `org.webjars:webjars-locator-lite`, el `test-jar` de `usuarios-core` en ámbito `test` y `spring-boot-maven-plugin`
- [X] T005 [P] Crear `.gitignore` (target/, IDE, `**/application-local.yml`, `.env`, logs) y `.gitattributes` (`* text=auto eol=lf`, `*.cmd` y `*.bat` con `eol=crlf`, binarios marcados)
- [X] T006 [P] Crear `.github/workflows/ci.yml`: en push y pull request, JDK 25 (Temurin), servicio `postgres:18` con base de datos `usuarios_test`, caché de Maven y `./mvnw -B verify` con las variables `SPRING_DATASOURCE_*` del servicio
- [X] T007 [P] Crear `.github/workflows/codeql.yml` (research R19): en push a `main`, en pull requests a `main` y semanal; lenguaje `java-kotlin`; `build-mode: manual` con JDK 25 (Temurin) y `./mvnw -B -DskipTests package`; consultas `security-extended`. Si CodeQL no admite aún Java 25, usar el JDK más alto admitido solo para el análisis
- [X] T008 [P] Crear `.github/dependabot.yml` con actualizaciones semanales para `maven` y `github-actions`
- [X] T009 [P] Crear `README.md` con: propósito de la plantilla; requisitos; enlace a `specs/001-registro-acceso/quickstart.md`; aviso de que los textos legales de ejemplo deben sustituirse y revisarse por un profesional competente; **Hoja de ruta** (002 roles, administración de usuarios y menús por rol; 003 auditoría de eventos de seguridad, principio VIII; 004 exportación y supresión de datos, principio IV; 005 MFA); y sección **Créditos** con la atribución de la lista de contraseñas prohibidas (SecLists, licencia MIT)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: infraestructura que necesitan todas las historias

**⚠️ CRITICAL**: ninguna historia puede empezar hasta completar esta fase

### Configuración y arranque

- [X] T010 Crear `core/config/UsuariosProperties.java`: `@ConfigurationProperties("usuarios")` con `@Validated`, con todas las propiedades y valores por defecto de `contracts/configuracion.md` (`url-publica` y `email.remitente` obligatorias con `@NotBlank`; duraciones como `Duration`)
- [X] T011 [P] Crear `core/config/RelojConfig.java` que expone un bean `Clock` UTC, sustituible en tests
- [X] T012 [P] Crear `core/config/AsincroniaConfig.java` con `@EnableAsync` y un `ThreadPoolTaskExecutor` acotado (núcleo 2, máximo 4, cola 100) llamado `ejecutorEmail`, y `@EnableScheduling`
- [X] T013 Crear `web/UsuariosApplication.java` (`@SpringBootApplication` que escanea `com.generico.usuarios`, extiende `SpringBootServletInitializer`) y `web-res/application.yml` (`spring.jpa.hibernate.ddl-auto: validate`, `open-in-view: false`, `spring.messages.basename: messages,usuarios/mensajes`, `server.error.include-stacktrace/include-message/include-exception: never`, actuator solo `health` sin detalles, logs a consola, propiedades `usuarios.*` leídas de variables de entorno)
- [X] T014 [P] Crear `web-res/application-local.example.yml` con valores ficticios (base de datos `usuarios` en `localhost:5432`, usuario `usuarios`, contraseña `cambia-esta-clave`, SMTP `localhost:1025`, `usuarios.url-publica: http://localhost:8080`, remitente `no-responder@ejemplo.invalid`)

### Base de datos y dominio

- [X] T015 Crear la migración `core-res/db/migration/V1__usuarios.sql` con las tablas `usuario`, `consentimiento` y `enlace_un_uso` exactamente como en `data-model.md`: `usuario.nombre varchar(100) not null`, `usuario.email varchar(254) not null unique`, `hash_contrasena varchar(255) not null`, `estado varchar(20) not null` con `CHECK (estado IN ('PENDIENTE_VERIFICACION','ACTIVO'))`, `intentos_fallidos integer not null default 0`, `version integer not null`; `consentimiento.tipo_documento` con `CHECK IN ('CONDICIONES_USO','POLITICA_PRIVACIDAD')`; `enlace_un_uso.finalidad` con `CHECK IN ('VERIFICACION_EMAIL','RECUPERACION_CONTRASENA')` y `hash_valor char(64) not null unique`; FK con `ON DELETE CASCADE`; todas las fechas `timestamptz`; índices `usuario(estado, creado_en)` y `enlace_un_uso(usuario_id, finalidad)`
- [X] T016 [P] Crear los enums `core/dominio/EstadoUsuario.java`, `core/dominio/TipoDocumento.java` y `core/dominio/FinalidadEnlace.java` con los valores de `data-model.md`
- [X] T017 [P] Escribir `core-test/dominio/UsuarioBloqueoTest.java`: el quinto fallo fija `bloqueado_hasta` a +15 min y reinicia el contador; un fallo durante el bloqueo no lo amplía; `registrarExito` reinicia contador y bloqueo; `estaBloqueada` es falso pasado el plazo; el email se normaliza con `trim()` y minúsculas
- [X] T018 Crear la entidad `core/dominio/Usuario.java` (id `UUID`, campos de `data-model.md`, `@Version`, email normalizado con `trim()` y `toLowerCase(Locale.ROOT)` en el constructor y en el setter, métodos de dominio `verificar(Instant)`, `registrarFallo(Instant, int maxIntentos, Duration bloqueo)`, `registrarExito()`, `estaBloqueada(Instant)` y `cambiarHashContrasena(String, Instant)`; no `final` para permitir extensión) hasta que pase T017
- [X] T019 [P] Crear las entidades `core/dominio/Consentimiento.java` y `core/dominio/EnlaceUnUso.java` según `data-model.md` (con `esValido(Instant)` en `EnlaceUnUso`)
- [X] T020 Crear los repositorios `core/repositorio/UsuarioRepositorio.java` (`findByEmail`, borrado de pendientes anteriores a una fecha), `core/repositorio/ConsentimientoRepositorio.java` y `core/repositorio/EnlaceUnUsoRepositorio.java` (consulta `@Modifying` de consumo atómico `UPDATE ... SET usado_en = :ahora WHERE hash_valor = :hash AND finalidad = :finalidad AND usado_en IS NULL AND invalidado_en IS NULL AND caduca_en > :ahora`, invalidación de enlaces vigentes por usuario y finalidad, y borrado de enlaces usados, invalidados o caducados hace más de 7 días)

### Contraseñas y enlaces

- [X] T021 [P] Escribir `core-test/contrasena/PoliticaContrasenasTest.java`: rechaza 11 caracteres y acepta 12; rechaza 129; cuenta puntos de código Unicode tras NFKC; acepta espacios y caracteres no latinos; rechaza una contraseña de la lista sin distinguir mayúsculas; rechaza si contiene la parte local del email o el nombre; sin reglas de composición por defecto; con `exigir-mayuscula` y `exigir-caracter-especial` activados las exige
- [X] T022 Implementar `core/contrasena/PoliticaContrasenas.java` (devuelve la lista de incumplimientos con claves de mensaje) y añadir `core-res/contrasenas-prohibidas.txt` con las 10.000 contraseñas de SecLists `Passwords/Common-Credentials/10k-most-common.txt` (licencia MIT; verificar la licencia vigente), con cabecera de comentario que indique fuente, licencia y fecha de descarga
- [X] T023 [P] Escribir `core-test/enlace/ServicioEnlacesIT.java`: el valor enviado tiene 43 caracteres Base64 URL; en base de datos solo está su SHA-256; consumir dos veces falla la segunda; caducado, invalidado o manipulado no se consume; emitir uno nuevo invalida los anteriores de la misma finalidad; dos consumos concurrentes del mismo enlace dan un solo éxito
- [X] T024 Implementar `core/enlace/ServicioEnlaces.java` (32 bytes de `SecureRandom`, Base64 URL sin relleno, hash SHA-256 en hexadecimal, `emitir(usuario, finalidad, validez)`, `consumir(valor, finalidad)` que devuelve `Optional<Usuario>`)
- [X] T025 Configurar en `core/config/SeguridadConfig.java` el bean `PasswordEncoder` como `DelegatingPasswordEncoder` con Argon2id (`Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()` o el equivalente vigente en Spring Security 7) como codificador por defecto con id `argon2`

### Email

- [X] T026 Crear el puerto `core/email/NotificadorEmail.java` y el registro `core/email/MensajeEmail.java` (destinatario, plantilla, variables) según `contracts/emails.md`
- [X] T027 Implementar `core/email/NotificadorEmailSmtp.java` con `JavaMailSender` y un `TemplateEngine` de Thymeleaf propio para emails con dos resolvedores (research R18): primero `classpath:/personalizado/email/` con `checkExistence=true` y orden 1, después `classpath:/templates/usuarios/email/` con orden 2; renderiza `{plantilla}.html` y `{plantilla}.txt`; asunto desde `MessageSource` con la clave `email.{plantilla}.asunto`; remitente de `UsuariosProperties`. Sin `@Component`: se declara como `@Bean` con `@ConditionalOnMissingBean(NotificadorEmail.class)` en `core/config/ConfiguracionPorDefecto.java` (`@AutoConfiguration`, registrada en `core-res/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`), porque esa condición solo es fiable en autoconfiguraciones, que se evalúan después de los beans del proyecto derivado
- [X] T028 Crear `core/email/SolicitudEmail.java` (evento de aplicación) y `core/email/OyenteSolicitudEmail.java` con `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` y `@Async("ejecutorEmail")` que llama al puerto y registra fallos en log solo con el id del usuario, sin el contenido del mensaje, el email ni el enlace
- [X] T029 [P] Crear la plantilla base `core-res/templates/usuarios/email/fragmentos.html`, la plantilla `core-res/templates/usuarios/email/contrasena-cambiada.html` y `.txt` según `contracts/emails.md` (la usan US3 y US4) y `core-res/usuarios/mensajes.properties` con los asuntos y textos comunes de los emails
- [X] T030 [P] Crear en el test-jar `core-test/email/NotificadorEmailEnMemoria.java` (implementación del puerto que guarda los mensajes, con método para extraer el enlace de un mensaje) y `core-test/soporte/ConfiguracionTest.java` (`@TestConfiguration` que la registra como `@Primary`, un `Clock` mutable `RelojControlado` y un ejecutor síncrono para que los emails se envíen en el mismo hilo)

### Seguridad y sesiones

- [X] T031 Completar `core/config/SeguridadConfig.java` con el `SecurityFilterChain` base: rutas públicas exactamente las de `contracts/rutas-web.md` (incluidos `/webjars/**`, `/css/**`, `/img/**`, `/favicon.ico`, `/error`, `/actuator/health`) y `anyRequest().authenticated()`; CSRF activo; `formLogin` con página `/acceso`; `logout` solo por `POST /salir` hacia `/acceso?salida`; cabeceras CSP `default-src 'self'; frame-ancestors 'none'; form-action 'self'`, HSTS, `nosniff`, `Referrer-Policy: same-origin`; fijación de sesión con `changeSessionId`; `sessionManagement` con `SessionRegistry` y sin límite de sesiones concurrentes
- [X] T032 Añadir a `core/config/ConfiguracionPorDefecto.java` (T027, research R6) `SessionRegistryImpl` con `@ConditionalOnMissingBean(SessionRegistry.class)` y una clase anidada con `@ConditionalOnClass(FindByIndexNameSessionRepository.class)` que registra `SpringSessionBackedSessionRegistry`; y crear `core/config/SesionConfig.java` con `HttpSessionEventPublisher` y un `ServletContextInitializer` que fija la inactividad desde `usuarios.sesion.inactividad`, la cookie `HttpOnly`, `Secure`, `SameSite=Lax`, nombre `USUARIOS_SESION` y seguimiento solo por cookie (válido también en Tomcat externo, research R3)
- [X] T033 Implementar `core/seguridad/ServicioSesiones.java`: `expirarTodas(usuarioId)` y `expirarTodasSalvo(usuarioId, idSesionActual)` usando el `SessionRegistry` (lo usan US3 y US4)

### Web base

- [X] T034 [P] Crear `web-res/templates/layout.html` (fragmento Thymeleaf con `lang="es"`, Bootstrap desde `/webjars/bootstrap/css/bootstrap.min.css` sin versión en la ruta, sin estilos ni scripts en línea, enlace "saltar al contenido", región de mensajes con `role="status"`) y `web-res/static/css/aplicacion.css`
- [X] T035 [P] Crear `web-res/templates/error.html` genérica (sin estado, traza ni mensaje interno) y `web-res/messages.properties` con los textos comunes de las vistas en español
- [X] T036 Crear `core-test/AplicacionPruebasCore.java` (`@SpringBootApplication` en el paquete `com.generico.usuarios.core`, para que los `@SpringBootTest` del core encuentren su configuración; excluida del test-jar, T003), `core-test/soporte/BaseIT.java` (clase base con `@SpringBootTest`, perfil `test`, `ConfiguracionTest` importada y limpieza de las tablas antes de cada test) y `usuarios-core/src/test/resources/application-test.yml` apuntando a `jdbc:postgresql://localhost:5432/usuarios_test` con credenciales por variables de entorno y valores por defecto de desarrollo
- [X] T037 [P] Escribir `core-test/email/PersonalizacionPlantillasIT.java` (research R18) con el `NotificadorEmailSmtp` real y un `JavaMailSender` simulado. Para no interferir con otros tests (los recursos de test viajan en el test-jar), usar solo nombres exclusivos de test: una plantilla `prueba` en `usuarios-core/src/test/resources/templates/usuarios/email/` y otra `prueba` en `usuarios-core/src/test/resources/personalizado/email/` → se usa la personalizada; la plantilla real `verificacion`, sin fichero personalizado → se usa la del core; con `@TestPropertySource(properties = "spring.messages.basename=mensajes-prueba,usuarios/mensajes")` y un `mensajes-prueba.properties` que redefine `email.verificacion.asunto`, prevalece ese asunto
- [X] T038 [P] Escribir `web-test/SeguridadBaseIT.java` (MockMvc): una ruta privada inexistente sin sesión redirige a `/acceso`; `/actuator/health` responde `UP` sin detalles; las cabeceras de seguridad de `contracts/rutas-web.md` están presentes; un POST sin token CSRF da 403 con la página de error genérica; un error 500 no muestra trazas

**Checkpoint**: base lista; las historias pueden empezar

**Notas de implementación (fases 1 y 2)**, desviaciones respecto al texto de las tareas:

- T003: `usuarios-core` depende también de `spring-boot-starter-webmvc` (filtros, `SecurityFilterChain` y `ServletContextInitializer` necesitan la pila servlet) y de `spring-session-core` como `optional`, solo para compilar el registro de sesiones compartidas de T032.
- T012: el ejecutor `ejecutorEmail` está en `ConfiguracionPorDefecto` con `@ConditionalOnMissingBean(name = "ejecutorEmail")`, para que los tests lo sustituyan por uno síncrono sin sobrescribir beans.
- T013: `UsuariosApplication` está en el paquete `com.generico.usuarios` (en `usuarios-web`), no en `.web`: así el escaneo, las entidades y los repositorios JPA del core se encuentran sin `@EntityScan` ni `@EnableJpaRepositories`.
- T015: `usuario.estado` es `varchar(30)`, porque `PENDIENTE_VERIFICACION` tiene 22 caracteres (corregido también en `data-model.md`).
- T025: Argon2id con los parámetros mínimos de OWASP (19 MiB, 2 iteraciones, paralelismo 1); `defaultsForSpringSecurity_v5_8()` usa 16 MiB.
- T033: crea también el principal `core/seguridad/UsuarioAutenticado.java` (id, email y nombre), que usará T058. Se añade `ServicioSesionesTest`.
- T037: como `verificacion` aún no existe (T044), el caso «plantilla del core sin personalizar» usa `contrasena-cambiada`.

---

## Phase 3: User Story 1 - Registrarse y verificar el email (Priority: P1) 🎯 MVP

**Goal**: una persona sin cuenta se registra con consentimiento y verifica su email.

**Independent Test**: registrar una cuenta, abrir el enlace capturado por `NotificadorEmailEnMemoria`
y comprobar que la cuenta queda `ACTIVO` y que hay dos consentimientos con la versión configurada.

### Tests for User Story 1 ⚠️

> Escribir primero y comprobar que fallan

- [X] T039 [P] [US1] Escribir `core-test/servicio/ServicioRegistroIT.java`: alta crea usuario `PENDIENTE_VERIFICACION` con hash `{argon2}` y dos consentimientos con `version_documento` de la configuración y `aceptado_en` del reloj; envía `verificacion`; email con mayúsculas y espacios se normaliza; email existente no crea cuenta y envía `registro-existente`; contraseña inválida lanza error de validación sin crear nada
- [X] T040 [P] [US1] Escribir `core-test/servicio/ServicioVerificacionIT.java`: enlace válido pone `ACTIVO` y `verificado_en`; enlace de cuenta ya verificada devuelve "ya verificada"; caducado a las 24 h + 1 s, usado o manipulado devuelve "no válido"; el reenvío invalida los anteriores; el reenvío para email inexistente o ya verificado no envía nada y no lanza error
- [X] T041 [P] [US1] Escribir `core-test/servicio/LimpiezaCuentasIT.java`: borra cuentas pendientes de más de 7 días con sus consentimientos y enlaces; conserva las de 6 días y todas las `ACTIVO`; borra enlaces usados, invalidados o caducados hace más de 7 días
- [X] T042 [P] [US1] Escribir `web-test/controlador/RegistroControladorIT.java`: `GET /registro` y `POST /registro` permitidos sin sesión; con datos válidos redirige a `/registro/enviado`; con email existente redirige exactamente igual y con el mismo cuerpo final; sin marcar casillas devuelve 200 con error por campo; la contraseña no aparece nunca en la respuesta; sin CSRF da 403; con `usuarios.registro.habilitado=false` las rutas de registro dan 404
- [X] T043 [P] [US1] Escribir `web-test/controlador/VerificacionControladorIT.java`: `GET /verificacion?t=` válido, ya verificado y no válido muestran los tres mensajes; `GET/POST /verificacion/reenvio` permitidos sin sesión y con la misma redirección exista o no el email; `/legal/condiciones` y `/legal/privacidad` permitidos sin sesión

### Implementation for User Story 1

- [X] T044 [P] [US1] Crear las plantillas `core-res/templates/usuarios/email/verificacion.html`/`.txt` y `core-res/templates/usuarios/email/registro-existente.html`/`.txt` según `contracts/emails.md`, con sus asuntos y textos en `core-res/usuarios/mensajes.properties`
- [X] T045 [US1] Implementar `core/servicio/ServicioRegistro.java`: valida con `PoliticaContrasenas`; si el email existe publica `SolicitudEmail` de `registro-existente`; si no, crea el usuario, los dos consentimientos y el enlace de verificación (validez `usuarios.verificacion.validez`) y publica `verificacion`; en ambos casos calcula un hash Argon2 para igualar tiempos y devuelve sin indicar el caso
- [X] T046 [US1] Implementar `core/servicio/ServicioVerificacion.java` (`verificar(valor)` con resultado `VERIFICADA`, `YA_VERIFICADA` o `NO_VALIDO`; `reenviar(email)` silencioso si no aplica)
- [X] T047 [US1] Implementar `core/servicio/LimpiezaCuentasPendientes.java` con `@Scheduled(cron = "${usuarios.limpieza.cron}")` usando el plazo `usuarios.verificacion.borrar-pendientes-tras` y el `Clock`
- [X] T048 [P] [US1] Crear `web/formulario/FormularioRegistro.java` con Bean Validation (`nombre` 1–100 tras recortar, `email` formato y ≤ 254, `contrasena` no vacía, `aceptaCondiciones` y `aceptaPrivacidad` `@AssertTrue`) y `web/formulario/FormularioEmail.java` (`email` con formato y ≤ 254), usado en el reenvío de verificación y en la recuperación
- [X] T049 [US1] Implementar `web/controlador/ControladorRegistro.java` (`GET/POST /registro`, `GET /registro/enviado`; 404 si el registro está deshabilitado; errores de `PoliticaContrasenas` mapeados al campo `contrasena`; patrón Post/Redirect/Get)
- [X] T050 [US1] Implementar `web/controlador/ControladorVerificacion.java` (`GET /verificacion`, `GET/POST /verificacion/reenvio` con `FormularioEmail`, `GET /verificacion/reenvio/enviado`)
- [X] T051 [P] [US1] Implementar `web/controlador/ControladorLegal.java` y las plantillas `web-res/templates/legal/condiciones.html` y `web-res/templates/legal/privacidad.html` con texto de ejemplo marcado claramente como "TEXTO DE EJEMPLO: sustituir y revisar por un profesional competente" y la versión configurada
- [X] T052 [US1] Crear las vistas `web-res/templates/registro.html` (requisitos de contraseña visibles, casillas sin marcar con enlaces a los textos legales, errores con `aria-describedby` y resumen de errores enfocado), `registro-enviado.html`, `verificacion.html` y `verificacion-reenvio.html`, y sus textos en `web-res/messages.properties`

**Checkpoint**: la historia 1 funciona y se prueba de forma independiente

**Notas de implementación (historia 1)**:

- T042: el caso «registro desactivado» va en su propia clase, `RegistroDesactivadoIT`, porque necesita otro contexto de Spring.
- T045: `ServicioRegistro` reutiliza `ServicioVerificacion.enviarEnlace` para no duplicar la emisión y el envío del enlace.
- T046: «ya verificada» se devuelve cuando el enlace es válido pero la cuenta ya estaba `ACTIVO` (por ejemplo, verificada al recuperar la contraseña); un enlace ya usado da «no válido», como pide el escenario 6.
- T052: en lugar de `registro-enviado.html` y `verificacion.html` hay una vista genérica `aviso.html` (título, texto y enlace opcional) que reutilizarán también US2 y US3. Los fragmentos comunes de formulario (resumen de errores con foco y requisitos de la contraseña) están en `fragmentos.html`. El idioma de la interfaz queda fijo en español (`spring.web.locale-resolver: fixed`).

---

## Phase 4: User Story 2 - Iniciar y cerrar sesión (Priority: P1)

**Goal**: acceso con email y contraseña, bloqueo temporal, limitación por origen, caducidad de
sesión y cierre de sesión.

**Independent Test**: con una cuenta `ACTIVO` creada directamente en base de datos, iniciar sesión,
ver `/inicio` con el nombre, cerrar sesión y comprobar que `/inicio` redirige a `/acceso`.

### Tests for User Story 2 ⚠️

- [X] T053 [P] [US2] Escribir `core-test/seguridad/LimitadorPeticionesTest.java`: 20 peticiones en un minuto pasan y la 21 se rechaza; el rechazo dura 5 minutos; otra IP no se ve afectada; la ventana se reinicia al minuto; `X-Forwarded-For` solo se usa si el proxy está en `proxies-confianza`
- [X] T054 [P] [US2] Escribir `web-test/controlador/AccesoIT.java`: `GET /acceso` permitido sin sesión; credenciales correctas llevan a `/inicio` con el nombre; email inexistente y contraseña errónea dan la misma redirección `/acceso?error` y el mismo cuerpo; cuenta pendiente con contraseña correcta va a `/acceso?pendiente` y con contraseña errónea a `/acceso?error`; tras 5 fallos la contraseña correcta da `/acceso?error` y se envía `cuenta-bloqueada`; a los 15 min + 1 s vuelve a entrar; tras pedir `/inicio` sin sesión y autenticarse se vuelve a `/inicio`; el identificador de sesión cambia al iniciar sesión
- [X] T055 [P] [US2] Escribir `web-test/controlador/SesionIT.java`: `/inicio` denegado sin sesión (302 a `/acceso`) y permitido con sesión; `POST /salir` invalida la sesión; `GET /salir` no cierra la sesión; una sesión con más de 12 h según `RelojControlado` redirige a `/acceso`; la inactividad configurada se aplica a la sesión creada; dos sesiones simultáneas del mismo usuario son válidas a la vez (FR-037)
- [X] T056 [P] [US2] Escribir `web-test/seguridad/LimitePeticionesIT.java`: la petición 21 en un minuto a `POST /acceso`, `POST /registro`, `POST /recuperacion` y `POST /verificacion/reenvio` devuelve 429 con la página `demasiados-intentos`, sin información de cuentas; los `GET` no se limitan

### Implementation for User Story 2

- [X] T057 [P] [US2] Crear la plantilla `core-res/templates/usuarios/email/cuenta-bloqueada.html`/`.txt` según `contracts/emails.md`, con su asunto y textos en `core-res/usuarios/mensajes.properties`
- [X] T058 [US2] Implementar `core/seguridad/ServicioDetallesUsuario.java` (`UserDetailsService` que busca por email normalizado y devuelve un `UserDetails` siempre habilitado y no bloqueado, con el id y el nombre del usuario accesibles para las vistas)
- [X] T059 [US2] Implementar `core/seguridad/ComprobacionesPosteriores.java` (`UserDetailsChecker` para `postAuthenticationChecks`: si `estaBloqueada` lanza `BadCredentialsException`; si está `PENDIENTE_VERIFICACION` lanza `CuentaPendienteException` propia) y registrar en `SeguridadConfig` un `DaoAuthenticationProvider` con el `PasswordEncoder`, comprobaciones previas vacías y estas posteriores
- [X] T060 [US2] Implementar `core/seguridad/OyenteEventosAutenticacion.java`: en `AuthenticationFailureBadCredentialsEvent` llama a `Usuario.registrarFallo` (si la cuenta existe y no está ya bloqueada) y, si se acaba de bloquear, publica `SolicitudEmail` de `cuenta-bloqueada`; en `AuthenticationSuccessEvent` llama a `registrarExito`; sin registrar el email en log
- [X] T061 [US2] Implementar `core/seguridad/ManejadorFalloAcceso.java` (`AuthenticationFailureHandler`: `CuentaPendienteException` → `/acceso?pendiente`, cualquier otro fallo → `/acceso?error`) y conectarlo en el `formLogin` de `SeguridadConfig`
- [X] T062 [US2] Implementar `core/seguridad/FiltroDuracionMaximaSesion.java` (guarda la hora de inicio de sesión del `Clock` en la sesión e invalida y redirige a `/acceso` al superar `usuarios.sesion.duracion-maxima`) y registrarlo en `SeguridadConfig`
- [X] T063 [US2] Implementar `core/seguridad/LimitadorPeticiones.java` (ventana fija por IP y grupo de rutas, en memoria con limpieza periódica, usando el `Clock`) y `core/seguridad/FiltroLimitePeticiones.java` (solo `POST` de `/acceso`, `/registro`, `/recuperacion` y `/verificacion/reenvio`; 429 y vista `demasiados-intentos`), registrado antes de la autenticación en `SeguridadConfig`
- [X] T064 [P] [US2] Implementar `web/controlador/ControladorAcceso.java` (`GET /acceso` con los mensajes `error`, `pendiente`, `salida` y `recuperada`) y `web/controlador/ControladorInicio.java` (`GET /inicio` con el nombre del usuario autenticado)
- [X] T065 [US2] Crear las vistas `web-res/templates/acceso.html` (mensaje genérico de error, aviso de cuenta pendiente con enlace a `/verificacion/reenvio`, enlaces a registro y recuperación), `inicio.html` (nombre, enlace a `/cuenta/contrasena` y formulario `POST /salir` con CSRF) y `demasiados-intentos.html`, y sus textos en `web-res/messages.properties`

**Checkpoint**: historias 1 y 2 funcionan de forma independiente (MVP completo)

**Notas de implementación (historia 2)**:

- T059: el `DaoAuthenticationProvider` se declara como **único** `AuthenticationProvider` (bean). Si no, Spring crea otro por defecto como gestor padre, y tras un `BadCredentialsException` por bloqueo lo reintentaría sin nuestras comprobaciones, saltándose el bloqueo. El aviso informativo de Spring Security sobre esto se silencia en `application.yml`.
- T060: los fallos y aciertos bloquean la fila del usuario (`findParaActualizarBy…`, `PESSIMISTIC_WRITE`), para que los intentos simultáneos cuenten todos sin errores de versión.
- T062: el filtro, en la primera petición autenticada, anota la hora de inicio y fija también la inactividad de la sesión (vale igual en Tomcat externo). Las sesiones expiradas desde el registro (US3, US4) redirigen a `/acceso` (`expiredUrl`).
- T063: `LimitadorPeticiones.reiniciar()` vacía los contadores; lo usa `BaseIT` entre tests.
- T065: la página del freno es `templates/error/429.html`, la vista de error que Spring Boot usa para el estado 429, en lugar de `demasiados-intentos.html`: el filtro solo hace `sendError(429)`. Los mensajes de `/acceso` salen de los parámetros de la URL en la vista.

---

## Phase 5: User Story 3 - Recuperar una contraseña olvidada (Priority: P2)

**Goal**: recuperar el acceso mediante un enlace por email, cerrando todas las sesiones.

**Independent Test**: con una cuenta `ACTIVO`, pedir recuperación, usar el enlace capturado,
establecer una contraseña nueva y comprobar que la nueva funciona y la antigua no.

### Tests for User Story 3 ⚠️

- [X] T066 [P] [US3] Escribir `core-test/servicio/ServicioRecuperacionIT.java`: solicitud para email inexistente no envía nada y no lanza error; para email existente envía `recuperacion` con enlace válido 1 h e invalida los anteriores; completar actualiza el hash, consume el enlace, expira todas las sesiones (verificado con `ServicioSesiones` simulado), levanta el bloqueo, pone `ACTIVO` una cuenta pendiente y envía `contrasena-cambiada`; enlace a 1 h + 1 s, usado o manipulado devuelve "no válido"; contraseña que incumple la política no consume el enlace
- [X] T067 [P] [US3] Escribir `web-test/controlador/RecuperacionControladorIT.java`: todas las rutas de recuperación permitidas sin sesión; `POST /recuperacion` redirige igual exista o no el email; `GET /recuperacion/nueva` con enlace no válido muestra el mensaje y la opción de pedir otro; `POST /recuperacion/nueva` correcto redirige a `/acceso?recuperada` y una sesión previa del usuario deja de dar acceso a `/inicio`; confirmación distinta o contraseña inválida devuelve 200 con errores; sin CSRF da 403

### Implementation for User Story 3

- [X] T068 [P] [US3] Crear la plantilla `core-res/templates/usuarios/email/recuperacion.html`/`.txt` según `contracts/emails.md`, con su asunto y textos en `core-res/usuarios/mensajes.properties`
- [X] T069 [US3] Implementar `core/servicio/ServicioRecuperacion.java`: `solicitar(email)` (si existe, emite enlace `RECUPERACION_CONTRASENA` con `usuarios.recuperacion.validez` y publica `recuperacion`; siempre devuelve sin indicar el caso), `comprobar(valor)` sin consumir y `completar(valor, nuevaContrasena)` (valida política, consume el enlace, cambia el hash, `registrarExito`, verifica si estaba pendiente, `expirarTodas` y publica `contrasena-cambiada`)
- [X] T070 [P] [US3] Crear `web/formulario/FormularioNuevaContrasena.java` (`t`, `contrasena`, `confirmacion` con validación de coincidencia)
- [X] T071 [US3] Implementar `web/controlador/ControladorRecuperacion.java` (`GET/POST /recuperacion` con `FormularioEmail`, `GET /recuperacion/enviado`, `GET/POST /recuperacion/nueva`; el enlace viaja en campo oculto, nunca en la URL de redirección)
- [X] T072 [US3] Crear las vistas `web-res/templates/recuperacion.html`, `recuperacion-enviado.html` y `recuperacion-nueva.html` (requisitos de contraseña visibles y errores accesibles) y sus textos en `web-res/messages.properties`

**Checkpoint**: historias 1, 2 y 3 funcionan de forma independiente

**Notas de implementación (historia 3)**:

- T069: `ServicioEnlaces.titular` comprueba un enlace sin consumirlo; `completar` valida la política antes de consumir, así que una contraseña rechazada no gasta el enlace. El email `contrasena-cambiada` se envía desde `ServicioRecuperacion.avisarContrasenaCambiada`, que reutilizará la historia 4.
- T070: la coincidencia con la confirmación la comprueba el controlador (`ErroresContrasena`, compartido con el registro), para que el error salga en el campo `confirmacion`. El campo oculto `t` no se valida: un enlace vacío o inventado se trata como no válido.
- T072: la página «enviado» usa la vista genérica `aviso.html`.
- Al probar el cierre de sesiones apareció un fallo: el principal (`UsuarioAutenticado`) se comparaba por email, como `User`, y una cuenta nueva con el email de otra ya borrada se mezclaba con sus sesiones en el registro. Ahora se compara por id.

---

## Phase 6: User Story 4 - Cambiar la contraseña estando autenticado (Priority: P3)

**Goal**: el usuario cambia su contraseña indicando la actual; se cierran sus demás sesiones.

**Independent Test**: con dos sesiones del mismo usuario, cambiar la contraseña en una y comprobar
que esa sigue activa, la otra no, y que solo la nueva contraseña sirve.

### Tests for User Story 4 ⚠️

- [ ] T073 [P] [US4] Escribir `core-test/servicio/ServicioCambioContrasenaIT.java`: con la actual correcta cambia el hash, expira las demás sesiones y envía `contrasena-cambiada`; con la actual incorrecta no cambia nada y cuenta como intento fallido (el quinto bloquea la cuenta); nueva igual a la actual o que incumple la política se rechaza
- [ ] T074 [P] [US4] Escribir `web-test/controlador/CambioContrasenaIT.java`: `GET/POST /cuenta/contrasena` denegados sin sesión (302 a `/acceso`) y permitidos con sesión; cambio correcto redirige a `/inicio?contrasenaCambiada`, cambia el identificador de sesión, mantiene la sesión actual y otra sesión del mismo usuario pierde el acceso; errores devuelven 200 con mensajes por campo; sin CSRF da 403

### Implementation for User Story 4

- [ ] T075 [US4] Implementar `core/servicio/ServicioCambioContrasena.java` (`cambiar(usuarioId, actual, nueva, idSesionActual)`: verifica la actual con el `PasswordEncoder`, si falla aplica `registrarFallo` y publica `cuenta-bloqueada` si procede; valida política y que sea distinta; cambia el hash, `expirarTodasSalvo` y publica `contrasena-cambiada`)
- [ ] T076 [P] [US4] Crear `web/formulario/FormularioCambioContrasena.java` (`contrasenaActual`, `contrasena`, `confirmacion`)
- [ ] T077 [US4] Implementar `web/controlador/ControladorCuenta.java` (`GET/POST /cuenta/contrasena`; tras el cambio llama a `request.changeSessionId()` y redirige a `/inicio?contrasenaCambiada`)
- [ ] T078 [US4] Crear la vista `web-res/templates/cuenta-contrasena.html` (requisitos de la nueva contraseña visibles, FR-026, y errores accesibles), añadir el mensaje de confirmación en `inicio.html` y los textos en `web-res/messages.properties`

**Checkpoint**: todas las historias funcionan de forma independiente

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: comprobaciones que afectan a varias historias y cierre del flujo de integración

- [ ] T079 [P] Escribir `web-test/seguridad/NoEnumeracionTiemposIT.java`: para `POST /acceso`, `/registro`, `/recuperacion` y `/verificacion/reenvio`, 50 peticiones con email existente y 50 con inexistente tras un calentamiento; la diferencia de la media de tiempos es inferior a 50 ms (SC-003)
- [ ] T080 [P] Escribir `web-test/seguridad/LogsSinDatosSensiblesIT.java` con `OutputCaptureExtension`: tras registrar, iniciar sesión fallido, recuperar y cambiar contraseña, la salida de logs no contiene la contraseña, el valor de ningún enlace, el email ni el nombre usados (FR-027, FR-031)
- [ ] T081 [P] Escribir `web-test/seguridad/ReglasAccesoIT.java` parametrizado con todas las rutas de `contracts/rutas-web.md`: cada ruta pública responde sin sesión y cada ruta privada redirige a `/acceso` sin sesión y responde con sesión (principio V)
- [ ] T082 [P] Escribir `web-test/rendimiento/RendimientoIT.java` (research R20; aplicación en puerto aleatorio, `java.net.http.HttpClient`, sin dependencias nuevas): crea 100 usuarios `ACTIVO`, abre 100 sesiones con hilos virtuales (obteniendo el token CSRF de `/acceso`) y durante 60 s cada una pide `/inicio` y `/cuenta/contrasena`; el percentil 95 debe ser inferior a 2 s (SC-009). Solo se ejecuta con `./mvnw verify -Prendimiento`
- [ ] T083 Revisar la accesibilidad de todas las vistas de `web-res/templates/` frente a WCAG 2.1 AA (etiquetas asociadas, `aria-describedby`, foco en el resumen de errores, contraste, uso a 360 px sin desplazamiento horizontal) y corregir lo encontrado (SC-008)
- [ ] T084 [P] Documentar en `README.md` cómo extiende un proyecto derivado: propiedades `usuarios.*`, sustitución de emails en `personalizado/email/` y de textos en `messages.properties` (research R18), implementación propia de `NotificadorEmail`, rango de migraciones `V1000+` y paso a varias instancias añadiendo Spring Session JDBC (SC-010, con el límite por origen aplicado por instancia)
- [ ] T085 Tarea manual en GitHub (research R19): tras la primera ejecución correcta de `ci.yml` y `codeql.yml` en el pull request de esta feature, editar el ruleset `proteger-main` y activar "Require status checks to pass" con los checks de compilación y tests y de CodeQL
- [ ] T086 Ejecutar `./mvnw verify` completo, `./mvnw verify -Prendimiento` y los 10 escenarios de `specs/001-registro-acceso/quickstart.md`, incluido el despliegue del WAR en un Tomcat 11 si está disponible, y anotar el resultado en la descripción del pull request

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (fase 1)**: sin dependencias
- **Foundational (fase 2)**: depende de Setup; bloquea todas las historias
- **Historias (fases 3 a 6)**: dependen solo de Foundational; se pueden hacer en paralelo o por
  prioridad (US1 → US2 → US3 → US4)
- **Polish (fase 7)**: depende de todas las historias; T085 necesita además el pull request abierto
  con la CI y CodeQL ejecutados

### User Story Dependencies

- **US1 (P1)**: independiente tras Foundational
- **US2 (P1)**: independiente tras Foundational (sus tests crean la cuenta `ACTIVO` directamente en
  base de datos). T063 limita también las rutas `POST` de US1 y US3: si esas historias aún no
  existen, el filtro se aplica igualmente y sus tests las cubren cuando se implementen
- **US3 (P2)**: usa `ServicioSesiones` y la plantilla `contrasena-cambiada` (Foundational) y
  `FormularioEmail` (T048, US1); la redirección final a `/acceso` necesita US2 solo para la
  comprobación manual
- **US4 (P3)**: necesita una sesión iniciada, por lo que en la práctica se hace tras US2; sus tests
  pueden autenticar con `spring-security-test`

### Within Each User Story

- Tests primero, y deben fallar
- Plantillas y formularios → servicios → controladores → vistas
- Historia completa y probada antes de pasar a la siguiente prioridad

### Parallel Opportunities

- Setup: T005, T006, T007, T008 y T009 en paralelo tras T001–T004
- Foundational: T011, T012, T014, T016, T017, T019, T021, T023, T029, T030, T034, T035,
  T037 y T038 en paralelo cuando sus dependencias directas están hechas
- Cada historia: todos sus tests [P] en paralelo; plantillas de email y formularios [P] en paralelo
- Polish: T079, T080, T081, T082 y T084 en paralelo

---

## Parallel Example: User Story 1

```bash
# Tests de la historia 1 a la vez:
Task: "ServicioRegistroIT en core-test/servicio/ServicioRegistroIT.java"
Task: "ServicioVerificacionIT en core-test/servicio/ServicioVerificacionIT.java"
Task: "LimpiezaCuentasIT en core-test/servicio/LimpiezaCuentasIT.java"
Task: "RegistroControladorIT en web-test/controlador/RegistroControladorIT.java"
Task: "VerificacionControladorIT en web-test/controlador/VerificacionControladorIT.java"

# Piezas independientes de la implementación:
Task: "Plantillas de email verificacion y registro-existente"
Task: "FormularioRegistro y FormularioEmail en web/formulario/"
Task: "ControladorLegal y textos legales de ejemplo"
```

---

## Implementation Strategy

### MVP First (historias 1 y 2)

1. Fase 1: Setup
2. Fase 2: Foundational (crítica: bloquea todo)
3. Fase 3: historia 1 (registro y verificación)
4. Fase 4: historia 2 (acceso y cierre de sesión)
5. **Parar y validar**: escenarios 1 a 5 y 9 a 10 de `quickstart.md`
6. Abrir el pull request si el MVP es suficiente, o seguir

Las dos historias P1 forman juntas el producto mínimo: una cuenta sin poder entrar no aporta valor.

### Incremental Delivery

1. Setup + Foundational → base lista, CI y CodeQL en verde
2. + Historia 1 → registro verificable
3. + Historia 2 → MVP: registro y acceso
4. + Historia 3 → recuperación de contraseña
5. + Historia 4 → cambio de contraseña
6. Polish → validación completa, checks obligatorios en `main` y pull request

---

## Notes

- [P] = ficheros distintos y sin dependencias pendientes
- La etiqueta [Story] permite rastrear cada tarea hasta su historia
- Commit tras cada tarea o grupo lógico, con Conventional Commits
- Antes de los tests de integración debe existir la base de datos `usuarios_test` (ver
  `quickstart.md`)
- Evitar: tareas vagas, dos tareas sobre el mismo fichero en paralelo, dependencias entre historias
  que rompan su independencia
