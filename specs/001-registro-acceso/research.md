# Investigación (fase 0): Registro y acceso de usuarios

**Feature**: `001-registro-acceso` | **Fecha**: 2026-10-25 | **Plan**: [plan.md](plan.md)

Cada apartado sigue el formato Decisión / Justificación / Alternativas consideradas.

## R1. Versiones de la plataforma

- **Decisión**: Java 25 LTS, Spring Boot 4.1.x (4.1.2 al redactar este plan), que trae Spring
  Framework 7 y Spring Security 7. Maven 3.10 con Maven Wrapper (`mvnw`) en el repositorio.
  PostgreSQL 18 en local. Tomcat 11 (Servlet 6.1, Jakarta EE 11).
- **Justificación**: Boot 4.1 es la última versión estable publicada en Maven Central; Boot 4
  exige Jakarta EE 11, de ahí Tomcat 11. El wrapper garantiza la misma versión de Maven en todas
  las máquinas y en la CI.
- **Alternativas**: Boot 3.5 (descartado: fuera de soporte OSS y requeriría Spring Security 6);
  Gradle (descartado: la constitución fija Maven).

## R2. Estructura Maven y starters de Boot 4

- **Decisión**: POM padre `usuarios-parent` con dos módulos:
  - `usuarios-core` (JAR): dominio, servicios, repositorios, configuración de seguridad,
    migraciones Flyway, puerto de email y plantillas de email.
  - `usuarios-web` (WAR): controladores, vistas Thymeleaf, recursos estáticos y clase de arranque.
  Se usan los starters modulares de Boot 4 (web MVC, security, data JPA, Flyway, Thymeleaf,
  validation, mail, actuator) y sus starters de test correspondientes.
- **Justificación**: cumple el principio II (core aislado y extraíble como starter) y el principio
  VI (WAR + ejecutable). Boot 4 separó la autoconfiguración en módulos: hay que declarar
  explícitamente, por ejemplo, el starter de Flyway, o las migraciones no se ejecutan.
- **Alternativas**: un único módulo (descartado: incumple el principio II); tres módulos con
  dominio separado de la seguridad (descartado por simplicidad, principio III).

## R3. Despliegue WAR en Tomcat 11 y ejecutable

- **Decisión**: `usuarios-web` empaquetado como `war`, con la clase de arranque extendiendo
  `SpringBootServletInitializer` y el starter de Tomcat en ámbito `provided`. El mismo WAR arranca
  con `java -jar` (Tomcat embebido) o se despliega en un Tomcat 11 externo.
- **Justificación**: es el mecanismo estándar de Spring Boot; un solo artefacto para ambos modos.
- **Nota**: las propiedades `server.servlet.session.*` solo aplican al Tomcat embebido. El tiempo
  de inactividad y los atributos de la cookie de sesión se fijan también por código
  (`ServletContextInitializer`) para que el comportamiento sea idéntico en Tomcat externo.

## R4. Almacenamiento de contraseñas: Argon2id

- **Decisión**: `DelegatingPasswordEncoder` con Argon2id como codificador por defecto
  (`Argon2PasswordEncoder` con los valores por defecto de Spring Security 7), y dependencia
  `org.bouncycastle:bcprov-jdk18on`.
- **Justificación**: lo exige el principio I. La implementación de Argon2 de Spring Security
  depende de Bouncy Castle; es la única dependencia ajena a Spring de la feature y queda
  justificada aquí (principio III). El prefijo `{argon2}` permite migrar de algoritmo en el futuro.
- **Alternativas**: bcrypt (nativo, sin dependencias; descartado porque la constitución fija
  Argon2id); PBKDF2 (descartado por el mismo motivo).

## R5. Inicio de sesión sin enumeración y con bloqueo

- **Decisión**:
  - Inicio de sesión por formulario de Spring Security con `DaoAuthenticationProvider`.
  - El `UserDetails` siempre se presenta como habilitado y no bloqueado ante las comprobaciones
    previas; el bloqueo temporal y la verificación de email se comprueban en las comprobaciones
    **posteriores** (`postAuthenticationChecks`), es decir, solo tras verificar la contraseña.
  - Cuenta bloqueada → mismo mensaje genérico que credenciales incorrectas. Cuenta sin verificar
    con contraseña correcta → aviso de verificación pendiente con opción de reenvío.
  - El contador de fallos se actualiza con los eventos de autenticación de Spring Security
    (fallo por credenciales incorrectas y éxito). Al alcanzar 5 fallos se fija
    `bloqueado_hasta = ahora + 15 min` y se envía el email de aviso.
  - Para emails inexistentes, `DaoAuthenticationProvider` ya ejecuta un cálculo de hash ficticio
    que iguala los tiempos.
- **Justificación**: si el bloqueo o la verificación se comprobaran antes de la contraseña, la
  respuesta (o su tiempo, al saltarse el cálculo del hash) revelaría qué cuentas existen o están
  bloqueadas (FR-028, FR-029).
- **Alternativas**: comprobaciones previas estándar (descartado por lo anterior); bloqueo por
  retardo progresivo (descartado: la spec fija bloqueo de 15 minutos).

## R6. Gestión de sesiones

- **Decisión**:
  - Sesión `HttpSession` en memoria (una instancia, aclaración 1), cookie `HttpOnly`, `Secure`,
    `SameSite=Lax`, nombre propio y sin identificador en la URL.
  - Protección frente a fijación de sesión: cambio de identificador al iniciar sesión (por defecto
    en Spring Security) y al cambiar la contraseña.
  - Inactividad: 30 minutos. Duración máxima: filtro propio que guarda la hora de inicio de sesión
    y la invalida a las 12 horas.
  - Cierre de "todas las sesiones" o "las demás sesiones" de un usuario: `SessionRegistry` de Spring
    Security con `HttpSessionEventPublisher`, sin límite de sesiones concurrentes (FR-037).
- **Justificación**: todo es nativo de Spring. Para varias instancias (SC-010), Spring Session
  JDBC sustituye la sesión y el registro sin tocar código: la autoconfiguración
  `ConfiguracionPorDefecto` declara `SessionRegistryImpl` con `@ConditionalOnMissingBean`, y una
  configuración con
  `@ConditionalOnClass(FindByIndexNameSessionRepository.class)` registra
  `SpringSessionBackedSessionRegistry` en cuanto la dependencia está presente. Añadir la
  dependencia y su configuración queda fuera de esta feature.
- **Alternativas**: Spring Session JDBC desde el inicio (descartado por simplicidad, aclaración 1).
- **Nota**: los beans sustituibles por un derivado (`SessionRegistry`, `NotificadorEmail`) se
  declaran solo en `ConfiguracionPorDefecto`, una `@AutoConfiguration` registrada en
  `AutoConfiguration.imports`. `@ConditionalOnMissingBean` solo es fiable ahí, porque las
  autoconfiguraciones se evalúan después de los beans del proyecto; en una clase escaneada
  dependería del orden de escaneo. El resto del core se sigue cargando por escaneo de
  `com.generico.usuarios`.

## R7. Enlaces de un solo uso

- **Decisión**: 32 bytes de `SecureRandom` codificados en Base64 URL-safe; en base de datos solo se
  guarda su hash SHA-256. Consumo atómico con una actualización condicionada
  (`UPDATE ... SET usado_en = now() WHERE hash = ? AND usado_en IS NULL AND caduca_en > now()`).
  Al emitir un enlace nuevo de una finalidad se invalidan los anteriores de esa finalidad.
- **Justificación**: 256 bits de entropía no son adivinables (FR-030); un hash rápido es suficiente
  porque el valor es aleatorio y largo (no necesita un algoritmo lento como las contraseñas). La
  actualización condicionada resuelve el caso límite de dos usos simultáneos.
- **Alternativas**: JWT firmados (descartado: no se pueden revocar sin estado y añaden
  dependencia); guardar el valor en claro (descartado: incumple FR-030).

## R8. Envío de email sin filtrar información ni bloquear la respuesta

- **Decisión**: puerto `NotificadorEmail` en `usuarios-core` con una implementación SMTP basada en
  `JavaMailSender`. Los envíos se disparan con eventos de aplicación escuchados **después del
  commit** de la transacción y se ejecutan de forma **asíncrona** en un pool acotado. Las plantillas
  de email son plantillas Thymeleaf de texto y HTML, sobreescribibles por los proyectos derivados.
- **Justificación**: la respuesta al usuario es idéntica y tarda lo mismo exista o no la cuenta
  (FR-028, FR-029); un fallo del servidor de correo no deshace el registro (caso límite). En
  desarrollo, Mailpit (ejecutable de Windows) captura los correos en `localhost:1025`, con
  interfaz web en `localhost:8025`.
- **Alternativas**: envío síncrono (descartado: fuga de tiempos y acoplamiento al SMTP); cola
  persistente de envíos (descartado por simplicidad; queda como mejora futura).
- **Nota**: la ubicación de las plantillas y cómo las sustituye un proyecto derivado se definen en
  R18.

## R9. Limitación de peticiones por origen

- **Decisión**: filtro propio con ventana fija de 1 minuto por IP de origen y por grupo de
  endpoints (acceso, registro, recuperación, reenvío), en memoria (`ConcurrentHashMap` con limpieza
  periódica). Al superar 20 peticiones, respuesta 429 con la página genérica "espera unos minutos"
  durante 5 minutos. La IP se toma de la conexión; las cabeceras `X-Forwarded-For` solo se tienen
  en cuenta si se configura un proxy de confianza.
- **Justificación**: los requisitos son simples y para una instancia; implementarlo es poco código
  y evita una dependencia (principio III). Con varias instancias el límite es por instancia; un
  límite global corresponde al proxy inverso o a una feature posterior.
- **Alternativas**: Bucket4j (descartado: dependencia innecesaria para una ventana fija);
  limitación en el proxy inverso (complementaria, no sustituye: la plantilla no puede suponer que
  existe un proxy).

## R10. Política de contraseñas

- **Decisión**: validador propio en `usuarios-core`: longitud entre 12 y 128 caracteres (contados
  como puntos de código Unicode, tras normalización NFKC), lista de contraseñas prohibidas incluida
  como recurso (las 10.000 más comunes de SecLists, fichero
  `Passwords/Common-Credentials/10k-most-common.txt`, licencia MIT; verificar la licencia vigente
  al incorporarla y citar fuente, licencia y fecha en la cabecera del fichero y en el README;
  comparadas sin distinguir mayúsculas), rechazo si contiene el email (parte local) o el nombre, y reglas
  opcionales de mayúscula y carácter especial desactivadas por defecto.
- **Justificación**: FR-023 a FR-026; sin servicios externos (principio IV).
- **Alternativas**: Passay (descartado: dependencia para reglas que son pocas líneas); consulta a
  Have I Been Pwned (descartado: envía datos a un tercero).

## R11. Protección CSRF, cabeceras y páginas de error

- **Decisión**: CSRF activo con el repositorio de sesión por defecto (tokens en formularios
  Thymeleaf automáticamente); cierre de sesión solo por POST. Cabeceras: CSP estricta
  (`default-src 'self'`, sin scripts ni estilos en línea), HSTS, `X-Content-Type-Options`,
  `Referrer-Policy: same-origin`, `frame-ancestors 'none'`. Página de error propia sin trazas
  (`server.error.include-*` desactivado).
- **Justificación**: principio I y FR-033, FR-034.

## R12. Recursos estáticos (Bootstrap 5)

- **Decisión**: Bootstrap 5 mediante WebJar (`org.webjars:bootstrap`) servido por la propia
  aplicación, con el localizador de WebJars de Spring para no fijar la versión en las vistas. No se
  usa htmx en esta feature.
- **Justificación**: sin CDN (constitución), y Dependabot vigila la versión de Bootstrap como
  cualquier otra dependencia Maven.
- **Alternativas**: copiar los ficheros de Bootstrap al repositorio (descartado: actualizaciones
  manuales y sin alertas de seguridad).

## R13. Consentimientos y textos legales

- **Decisión**: las versiones vigentes de las condiciones y de la política de privacidad se
  configuran por propiedades (`usuarios.legal.condiciones.version`, etc.) y sus textos son
  plantillas Thymeleaf sobreescribibles. Cada aceptación se guarda en la tabla `consentimiento`.
- **Justificación**: FR-002 a FR-004; cada proyecto derivado cambia textos y versiones sin tocar
  código.

## R14. Borrado de cuentas sin verificar

- **Decisión**: tarea programada diaria (`@Scheduled`, expresión cron configurable) que borra las
  cuentas pendientes con más de 7 días; los consentimientos y enlaces se eliminan en cascada.
- **Justificación**: FR-010. En una instancia no hay ejecuciones concurrentes; si en el futuro hay
  varias, el borrado es idempotente y se podrá añadir un bloqueo distribuido.

## R15. Configuración y secretos

- **Decisión**: propiedades propias bajo el prefijo `usuarios.*` en una clase
  `@ConfigurationProperties` validada al arrancar. Credenciales de base de datos y SMTP por variables
  de entorno. Fichero `application-local.yml` excluido por `.gitignore` y un
  `application-local.example.yml` versionado con valores ficticios.
- **Justificación**: principios VI y VII; FR-036.

## R16. Estrategia de tests sin Docker

- **Decisión**:
  - Tests unitarios (JUnit 5) de la lógica de dominio: política de contraseñas, enlaces, bloqueo y
    limitación por origen.
  - Tests de integración `@SpringBootTest` + MockMvc + Spring Security Test contra una base de
    datos PostgreSQL local dedicada (`usuarios_test`), con Flyway aplicando las migraciones al
    arrancar.
  - El envío de email se sustituye en tests por una implementación en memoria del puerto
    `NotificadorEmail`, que permite leer los enlaces enviados.
  - El reloj (`java.time.Clock`) se inyecta para probar caducidades y bloqueos sin esperas.
- **Justificación**: principio V (PostgreSQL real, sin H2). Testcontainers se añadirá cuando haya
  Docker; la estructura de tests no cambia.

## R17. Accesibilidad e idioma

- **Decisión**: formularios con `label` asociados, mensajes de error enlazados con
  `aria-describedby`, foco en el primer error, contraste de Bootstrap por defecto y diseño
  responsive de Bootstrap. Textos en `messages.properties` (español), sobreescribibles.
- **Justificación**: SC-008 y FR-035.

## R18. Personalización de plantillas y textos

- **Decisión**:
  - Las plantillas de email del core viven en `templates/usuarios/email/` dentro de
    `usuarios-core`.
  - El motor de plantillas de email usa dos resolvedores en orden: primero
    `classpath:/personalizado/email/` (con comprobación de existencia, así que solo se usa si el
    fichero existe) y después el del core. Un proyecto derivado sustituye un email creando, por
    ejemplo, `usuarios-web/src/main/resources/personalizado/email/verificacion.html`.
  - Los textos del core (asuntos y contenido de los emails) van en `usuarios/mensajes.properties`.
    La aplicación declara `spring.messages.basename: messages,usuarios/mensajes`, de modo que una
    clave repetida en el `messages.properties` del proyecto prevalece sobre la del core.
  - Las vistas web viven en `usuarios-web`, que cada proyecto derivado copia y adapta directamente.
- **Justificación**: no depende del orden del classpath, que cambia entre el IDE, Maven y el WAR;
  cumple el principio II (personalizar sin modificar `usuarios-core`).
- **Alternativas**: misma ruta de plantilla en ambos módulos (descartado: gana la primera del
  classpath, comportamiento frágil y distinto según cómo se arranque).

## R19. Análisis de seguridad y protección de `main`

- **Decisión**: workflow `.github/workflows/codeql.yml` con CodeQL para `java-kotlin`, compilación
  manual con JDK 25 y `./mvnw`, consultas `security-extended`, en push y pull request a `main` y
  semanal. Tras su primera ejecución, el ruleset de `main` exige los checks de CI y CodeQL.
- **Justificación**: la constitución (principio VII y flujo de desarrollo) exige CodeQL activo y
  CI en verde para integrar. Un workflow versionado deja la configuración en el repositorio y es
  revisable.
- **Riesgo**: si CodeQL aún no admite Java 25, se compila para el análisis con la versión más alta
  que admita, sin cambiar el proyecto.
- **Alternativas**: configuración por defecto de CodeQL desde los ajustes de GitHub (descartado:
  no permite fijar el JDK ni queda versionada).

## R20. Prueba de rendimiento

- **Decisión**: test `RendimientoIT` con la aplicación en puerto aleatorio, `java.net.http.HttpClient`
  e hilos virtuales de Java 25, sin dependencias nuevas. Se excluye del build normal y se ejecuta
  con el perfil Maven `rendimiento`.
- **Justificación**: SC-009 debe comprobarse, pero una prueba de un minuto dependiente de la
  máquina haría la CI lenta e inestable.
- **Alternativas**: JMeter o Gatling (descartado: herramientas externas para una sola medición).
