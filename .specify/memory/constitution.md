# Constitución del proyecto «usuarios»

Plantilla pública y reutilizable de gestión de usuarios con Spring Boot y Spring Security,
pensada para exponerse a internet y servir de base a otros proyectos que hereden la
autenticación, la autorización y la administración de usuarios ya resueltas.
Paquete Java base: `com.generico.usuarios`.

## Principios fundamentales

### I. Seguridad por defecto (NO NEGOCIABLE)

- El proyecto DEBE cumplir OWASP ASVS nivel 2 y cubrir el OWASP Top 10.
- Autorización denegada por defecto: toda ruta DEBE requerir autenticación salvo las declaradas
  públicas de forma explícita.
- Las contraseñas DEBEN almacenarse con Argon2id mediante `DelegatingPasswordEncoder`, para poder
  migrar de algoritmo sin invalidar las existentes.
- La sesión DEBE viajar en una cookie `HttpOnly`, `Secure` y `SameSite`; la protección CSRF DEBE
  estar activa; DEBE existir protección frente a fijación de sesión.
- Sin enumeración de usuarios: login, registro y recuperación de contraseña DEBEN dar la misma
  respuesta, exista o no la cuenta.
- Los endpoints de autenticación DEBEN aplicar bloqueo temporal o limitación de intentos.
- Los tokens de verificación y de recuperación DEBEN ser de un solo uso, con caducidad y
  almacenados como hash.
- Las cabeceras de seguridad (CSP, HSTS, `X-Content-Type-Options`, `Referrer-Policy`) DEBEN estar
  activas.
- La entrada DEBE validarse en servidor; la salida DEBE escaparse en las vistas (NO se usa
  `th:utext` con datos del usuario); las consultas DEBEN ser siempre parametrizadas.
- Las páginas de error DEBEN ser genéricas y NO DEBEN mostrar trazas ni detalles internos.

**Justificación**: la aplicación se expone a internet y es la base de seguridad de todos los
proyectos derivados; un fallo aquí se replica en cada uno de ellos.

### II. Plantilla modular y extensible

- El código de gestión de usuarios DEBE vivir en un módulo propio (`usuarios-core`), aislado del
  módulo de aplicación (`usuarios-web`), para poder extraerlo como starter en el futuro.
- Los proyectos derivados DEBEN poder extender campos de usuario, roles, menús, plantillas de
  email y textos sin modificar el código de `usuarios-core`.
- Los servicios de negocio NO DEBEN depender de la capa web, para poder añadir en el futuro una
  API REST u otro frontend sin reescribir la lógica.

**Justificación**: el valor del proyecto está en reutilizarse; el aislamiento permite propagar
mejoras y parches a los derivados con el menor coste posible.

### III. Simplicidad

- Ante varias soluciones se DEBE elegir la más simple que cumpla los requisitos (YAGNI).
- Toda dependencia externa DEBE justificarse en el plan de la feature que la introduce.
- Se DEBEN preferir las capacidades nativas de Spring antes que librerías de terceros.

**Justificación**: cada dependencia añade superficie de ataque y mantenimiento que heredan todos
los proyectos derivados.

### IV. Privacidad y RGPD desde el diseño

- Solo se DEBEN recoger los datos personales imprescindibles (minimización).
- El consentimiento DEBE quedar registrado con fecha y versión del texto aceptado.
- El usuario DEBE poder exportar sus datos y solicitar su supresión o anonimización.
- Los logs y la auditoría NO DEBEN contener contraseñas, tokens ni datos personales innecesarios.
- Ningún dato de usuario se DEBE enviar a servicios externos sin una decisión expresa documentada.
- Los textos legales los aporta cada proyecto derivado y DEBEN ser revisados por un profesional
  competente; la plantilla solo provee el mecanismo.

**Justificación**: la plantilla trata datos personales de usuarios reales; cumplir el RGPD debe
ser el comportamiento por defecto, no una adaptación posterior.

### V. Calidad verificada con tests

- Toda funcionalidad de seguridad (autenticación, autorización, tokens, bloqueo) DEBE tener tests
  automatizados, escritos antes o junto con la implementación.
- Cada regla de acceso por rol DEBE tener un test que verifique tanto el acceso permitido como el
  denegado.
- Los tests de integración DEBEN ejecutarse contra PostgreSQL real; NO se usa H2 como sustituto.
- El build NO DEBE pasar si falla algún test.

**Justificación**: los errores de seguridad no se ven a simple vista; solo los tests garantizan
que una regla de acceso sigue cumpliéndose tras cada cambio.

### VI. Portabilidad (doce factores)

- Toda la configuración y los secretos DEBEN leerse de variables de entorno o de ficheros de
  configuración externos; nada fijo en el código.
- La aplicación NO DEBE guardar estado en el disco local del servidor; los logs DEBEN ir a la
  consola (stdout).
- DEBE existir un health check con Spring Boot Actuator, expuesto solo con la información mínima.
- El artefacto DEBE desplegarse como WAR en Tomcat 11 y también arrancar de forma autónoma con el
  Tomcat embebido, para facilitar una futura contenedorización.

**Justificación**: permite desplegar la misma aplicación en Tomcat, en contenedor o en cualquier
entorno sin cambiar el código.

### VII. Repositorio público

- NUNCA se DEBEN subir secretos, credenciales, claves ni datos reales al repositorio.
- DEBE incluirse un fichero de configuración de ejemplo con valores ficticios y un `.gitignore`
  que excluya la configuración local.
- Secret scanning con push protection, CodeQL y Dependabot DEBEN estar activos.

**Justificación**: todo lo que se sube a un repositorio público queda expuesto de forma
permanente, aunque se borre después.

### VIII. Auditoría y trazabilidad

- Los eventos de seguridad (login correcto y fallido, bloqueo, cambio de contraseña, cambios de
  rol, acciones de administración) DEBEN registrarse en una tabla de auditoría con fecha, actor,
  acción y resultado.
- El registro de auditoría DEBE ser de solo inserción desde la aplicación.

**Justificación**: sin trazabilidad no es posible investigar un incidente ni demostrar quién hizo
qué y cuándo.

## Stack y restricciones técnicas

- Java 25 LTS, Spring Boot 4.x, Spring Security 7, Spring Data JPA.
- Maven multi-módulo. Paquete base `com.generico.usuarios`.
- PostgreSQL como base de datos; Flyway para las migraciones (prohibido `ddl-auto` en entornos
  que no sean de test).
- Frontend renderizado en servidor: Thymeleaf + Bootstrap 5, con htmx solo donde aporte valor.
  Los recursos estáticos se sirven desde la propia aplicación, sin CDN.
- Menús y vistas condicionados por rol mediante la integración de Thymeleaf con Spring Security.
- Tests: JUnit 5, Spring Security Test y MockMvc. Los tests de integración usan el PostgreSQL
  instalado en local (base de datos de test dedicada); Testcontainers se incorporará cuando haya
  Docker disponible.
- Email por SMTP detrás de una interfaz propia; Mailpit en desarrollo.
- Aplicación de una sola organización (sin multi-tenant).

## Flujo de desarrollo

- Cada funcionalidad sigue el ciclo de Spec Kit: specify → clarify → plan → tasks → implement,
  en su propia rama.
- La rama `main` está protegida: solo se integra mediante pull request con la CI en verde.
- La CI en GitHub Actions DEBE compilar, ejecutar los tests y lanzar los análisis de seguridad en
  cada pull request.
- Los mensajes de commit siguen Conventional Commits.
- Código, nombres de dominio y documentación en español; los identificadores técnicos del
  framework siguen sus propias convenciones.

## Gobernanza

- Esta constitución prevalece sobre cualquier otra práctica del proyecto.
- Todo plan DEBE verificar su cumplimiento (Constitution Check) antes de implementarse; las
  excepciones DEBEN justificarse por escrito en el plan.
- Toda revisión de pull request DEBE comprobar el cumplimiento de los principios afectados.
- Las modificaciones se documentan en este fichero y se versionan con versionado semántico:
  MAJOR cuando se elimina o redefine un principio; MINOR cuando se añade un principio o sección o
  se amplía de forma relevante; PATCH para aclaraciones y correcciones de redacción.

**Version**: 1.0.0 | **Ratified**: 2026-10-24 | **Last Amended**: 2026-10-24
