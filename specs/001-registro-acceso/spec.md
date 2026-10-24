# Feature Specification: Registro y acceso de usuarios

**Feature Branch**: `001-registro-acceso`

**Created**: 2026-10-24

**Status**: Draft

**Input**: User description: "Registro y acceso de usuarios (feature 001): registro con consentimiento,
verificación de email, inicio y cierre de sesión con bloqueo por intentos fallidos, recuperación y
cambio de contraseña, política de contraseñas robusta, sin revelar si una cuenta existe, con
interfaz web renderizada en servidor."

## Clarifications

### Session 2026-10-24

- Q: ¿Qué volumen de uso y despliegue debe soportar la plantilla por defecto? → A: Una sola
  instancia, hasta 10.000 usuarios registrados y 100 sesiones simultáneas; sesiones en memoria,
  preparadas para compartirse entre instancias más adelante solo con configuración.
- Q: ¿Puede un mismo usuario tener la sesión abierta en varios dispositivos a la vez? → A: Sí, sin
  límite; cada dispositivo tiene su propia sesión.
- Q: ¿Debe la pantalla de inicio de sesión ofrecer la opción "recordarme"? → A: No en esta feature;
  la sesión caduca a los 30 minutos sin actividad y, como máximo, a las 12 horas aunque se use.
- Q: ¿Cuántas peticiones de inicio de sesión, registro o recuperación se permiten desde una misma
  conexión antes de frenarla? → A: 20 por minuto; al superarlo, se frena 5 minutos (configurable).
- Q: ¿Cuánto tiempo se conserva una cuenta que nunca verificó su email antes de borrarla? → A: 7 días
  desde el registro; se borra junto con sus consentimientos y enlaces (configurable).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrarse y verificar el email (Priority: P1)

Una persona que todavía no tiene cuenta rellena el formulario de registro con su nombre, su email y
una contraseña, y acepta las condiciones de uso y la política de privacidad. Recibe un email con un
enlace; al pulsarlo, su cuenta queda verificada y ya puede iniciar sesión.

**Why this priority**: sin cuentas no existe ninguna otra funcionalidad. Es la puerta de entrada de
la plantilla y la base de todos los proyectos derivados.

**Independent Test**: se registra una cuenta nueva, se abre el enlace recibido y se comprueba que la
cuenta pasa a estar verificada y que el consentimiento quedó registrado con fecha y versión.

**Acceptance Scenarios**:

1. **Given** una persona sin cuenta, **When** envía el formulario con datos válidos y acepta las
   condiciones y la política de privacidad, **Then** ve un mensaje indicando que, si los datos son
   correctos, recibirá un email para verificar la cuenta, y recibe ese email con un enlace.
2. **Given** una cuenta recién registrada sin verificar, **When** la persona abre el enlace de
   verificación dentro de su plazo de validez, **Then** la cuenta queda verificada y se le invita a
   iniciar sesión.
3. **Given** el formulario de registro, **When** la persona no acepta las condiciones o la política
   de privacidad, **Then** el registro no se completa y se le indica que la aceptación es obligatoria.
4. **Given** el formulario de registro, **When** la contraseña no cumple la política de contraseñas,
   **Then** el registro no se completa y se indica qué requisito no se cumple.
5. **Given** un email que ya pertenece a una cuenta, **When** alguien intenta registrarse con él,
   **Then** ve exactamente el mismo mensaje que en un registro correcto, no se crea ninguna cuenta y
   el titular de la cuenta existente recibe un email avisándole del intento, con un enlace a la
   recuperación de contraseña.
6. **Given** un enlace de verificación caducado o ya utilizado, **When** la persona lo abre, **Then**
   se le informa de que el enlace no es válido y se le ofrece solicitar uno nuevo.
7. **Given** una cuenta sin verificar, **When** la persona solicita reenviar el enlace de
   verificación, **Then** recibe un nuevo enlace y los anteriores dejan de ser válidos.

---

### User Story 2 - Iniciar y cerrar sesión (Priority: P1)

Una persona con la cuenta verificada introduce su email y su contraseña y accede a su página de
inicio. Cuando termina, cierra la sesión. Si alguien intenta adivinar la contraseña, la cuenta se
bloquea temporalmente.

**Why this priority**: junto con el registro forma el producto mínimo viable: crear cuenta y entrar.

**Independent Test**: con una cuenta verificada, se inicia sesión, se accede a la página de inicio,
se cierra sesión y se comprueba que la página de inicio ya no es accesible.

**Acceptance Scenarios**:

1. **Given** una cuenta verificada, **When** la persona introduce el email y la contraseña correctos,
   **Then** accede a su página de inicio, que muestra su nombre.
2. **Given** cualquier combinación de email y contraseña incorrecta (email inexistente o contraseña
   errónea), **When** se intenta iniciar sesión, **Then** se muestra el mismo mensaje genérico de
   credenciales incorrectas.
3. **Given** una cuenta sin verificar, **When** la persona introduce el email y la contraseña
   correctos, **Then** no accede, se le indica que debe verificar su email y se le ofrece reenviar
   el enlace.
4. **Given** una cuenta verificada, **When** se producen 5 intentos fallidos consecutivos, **Then** la
   cuenta queda bloqueada 15 minutos, durante ese tiempo ningún intento de acceso tiene éxito (aunque
   la contraseña sea correcta) y el titular recibe un email avisándole del bloqueo.
5. **Given** una sesión iniciada, **When** la persona cierra la sesión, **Then** la sesión deja de ser
   válida y cualquier página privada redirige al inicio de sesión.
6. **Given** una sesión sin actividad durante 30 minutos, **When** la persona intenta usarla, **Then**
   se le pide iniciar sesión de nuevo.
7. **Given** una sesión iniciada hace 12 horas y usada de forma continuada, **When** la persona
   realiza cualquier acción, **Then** se le pide iniciar sesión de nuevo.
8. **Given** una persona sin sesión, **When** intenta acceder a una página privada, **Then** se le
   redirige al inicio de sesión y, tras autenticarse, vuelve a la página que pidió.

---

### User Story 3 - Recuperar una contraseña olvidada (Priority: P2)

Una persona que no recuerda su contraseña indica su email y recibe un enlace para establecer una
nueva. Tras cambiarla, todas sus sesiones abiertas se cierran y debe iniciar sesión con la nueva.

**Why this priority**: sin recuperación, cada olvido de contraseña es una cuenta perdida o una
petición de soporte, pero el producto sigue siendo usable sin ella en una primera demostración.

**Independent Test**: con una cuenta verificada, se solicita la recuperación, se abre el enlace, se
establece una contraseña nueva y se comprueba que la nueva funciona y la antigua no.

**Acceptance Scenarios**:

1. **Given** cualquier email (exista o no una cuenta), **When** se solicita la recuperación, **Then**
   se muestra siempre el mismo mensaje indicando que, si existe una cuenta, se ha enviado un email.
2. **Given** una cuenta existente, **When** se solicita la recuperación, **Then** el titular recibe un
   enlace válido durante 1 hora y los enlaces de recuperación anteriores dejan de ser válidos.
3. **Given** un enlace de recuperación válido, **When** la persona establece una contraseña nueva que
   cumple la política, **Then** la contraseña se actualiza, el enlace deja de ser válido, todas las
   sesiones abiertas de esa cuenta se cierran, el titular recibe un email confirmando el cambio y se
   le lleva al inicio de sesión.
4. **Given** un enlace de recuperación caducado o ya utilizado, **When** la persona lo abre, **Then**
   se le informa de que no es válido y se le ofrece solicitar uno nuevo.
5. **Given** una cuenta bloqueada por intentos fallidos, **When** el titular completa la recuperación
   de contraseña, **Then** el bloqueo se levanta.
6. **Given** una cuenta sin verificar, **When** el titular completa la recuperación de contraseña,
   **Then** la cuenta queda también verificada, ya que el titular ha demostrado controlar el email.

---

### User Story 4 - Cambiar la contraseña estando autenticado (Priority: P3)

Una persona con sesión iniciada quiere cambiar su contraseña. Introduce la actual y la nueva; el
resto de sus sesiones se cierran y recibe un email confirmando el cambio.

**Why this priority**: es una buena práctica de higiene de seguridad, pero la recuperación de
contraseña ya cubre el caso crítico.

**Independent Test**: con una sesión iniciada, se cambia la contraseña y se comprueba que la nueva
funciona, la antigua no, y que otra sesión abierta de la misma cuenta deja de ser válida.

**Acceptance Scenarios**:

1. **Given** una sesión iniciada, **When** la persona introduce su contraseña actual correcta y una
   nueva que cumple la política, **Then** la contraseña se actualiza, la sesión actual continúa,
   las demás sesiones de la cuenta se cierran y el titular recibe un email confirmando el cambio.
2. **Given** una sesión iniciada, **When** la contraseña actual introducida es incorrecta, **Then** el
   cambio no se realiza y el intento cuenta como intento fallido a efectos de bloqueo.
3. **Given** una sesión iniciada, **When** la nueva contraseña es igual a la actual o no cumple la
   política, **Then** el cambio no se realiza y se indica el motivo.

---

### Edge Cases

- Emails escritos con distintas mayúsculas o espacios alrededor (`Ana@Ejemplo.com ` y
  `ana@ejemplo.com`) se tratan como la misma cuenta.
- Si el envío de un email falla, el registro o la solicitud no se pierde: la persona ve el mismo
  mensaje y puede solicitar el reenvío más tarde.
- Si se abre un enlace de verificación de una cuenta ya verificada, se informa de que la cuenta ya
  está verificada y se invita a iniciar sesión, sin error.
- Si un enlace (de verificación o recuperación) se abre dos veces a la vez, solo una de las dos
  operaciones tiene éxito.
- Un enlace manipulado o inventado se trata igual que uno caducado.
- Las cuentas que no se verifican en 7 días se eliminan automáticamente, liberando el email para un
  nuevo registro.
- Durante un bloqueo temporal, los intentos con un email inexistente reciben el mismo mensaje que con
  uno existente, para no revelar qué cuentas existen o están bloqueadas.
- Más de 20 intentos de acceso, registro o recuperación en un minuto desde un mismo origen se frenan
  durante 5 minutos, aunque vayan contra emails distintos (FR-014).
- Un formulario enviado desde otro sitio web (sin el token de protección del formulario original) se
  rechaza.
- El cierre de sesión no puede provocarse desde un enlace externo; requiere una acción explícita del
  usuario en la propia aplicación.

## Requirements *(mandatory)*

### Functional Requirements

**Registro y consentimiento**

- **FR-001**: El sistema DEBE permitir registrarse con nombre (1 a 100 caracteres), email y contraseña.
- **FR-002**: El sistema DEBE exigir la aceptación expresa de las condiciones de uso y de la política
  de privacidad para completar el registro; las casillas NO DEBEN venir marcadas por defecto.
- **FR-003**: El sistema DEBE registrar cada aceptación con la fecha y hora y la versión del texto
  aceptado.
- **FR-004**: Los textos legales y sus versiones DEBEN ser configurables por cada proyecto derivado
  sin modificar el código de la plantilla.
- **FR-005**: El sistema DEBE tratar el email sin distinguir mayúsculas ni espacios iniciales o
  finales, y NO DEBE permitir dos cuentas con el mismo email.
- **FR-006**: El registro DEBE poder desactivarse por configuración en los proyectos derivados que
  no admitan auto-registro.

**Verificación del email**

- **FR-007**: Tras el registro, el sistema DEBE enviar un enlace de verificación de un solo uso,
  válido durante 24 horas.
- **FR-008**: Una cuenta sin verificar NO DEBE poder iniciar sesión.
- **FR-009**: El sistema DEBE permitir solicitar el reenvío del enlace de verificación; cada enlace
  nuevo DEBE invalidar los anteriores.
- **FR-010**: El sistema DEBE eliminar de forma automática las cuentas no verificadas 7 días después
  del registro, junto con sus consentimientos y enlaces de un solo uso, sin dejar datos personales
  de esas cuentas.

**Inicio y cierre de sesión**

- **FR-011**: El sistema DEBE permitir iniciar sesión con email y contraseña a las cuentas
  verificadas.
- **FR-012**: El sistema DEBE bloquear la cuenta durante 15 minutos tras 5 intentos fallidos
  consecutivos, y DEBE notificar el bloqueo al titular por email.
- **FR-013**: El contador de intentos fallidos DEBE reiniciarse tras un inicio de sesión correcto.
- **FR-014**: El sistema DEBE limitar las peticiones de inicio de sesión, registro, recuperación y
  reenvío de verificación procedentes de un mismo origen: al superar 20 peticiones en un minuto, las
  siguientes peticiones de ese origen se rechazan durante 5 minutos con un mensaje que invita a
  esperar, sin revelar nada sobre las cuentas.
- **FR-015**: El sistema DEBE cerrar la sesión tras 30 minutos de inactividad y, en todo caso, a las
  12 horas de haberse iniciado aunque se haya usado de forma continuada. No se ofrece la opción
  "recordarme".
- **FR-016**: El sistema DEBE emitir un identificador de sesión nuevo al iniciar sesión y al cambiar
  la contraseña.
- **FR-017**: El sistema DEBE permitir cerrar sesión mediante una acción explícita del usuario,
  invalidando la sesión en el servidor.
- **FR-037**: El sistema DEBE permitir que un mismo usuario mantenga varias sesiones simultáneas,
  sin límite, en distintos dispositivos o navegadores; iniciar sesión en uno NO DEBE cerrar las
  sesiones de los demás.
- **FR-018**: Toda página que no sea pública (registro, inicio de sesión, recuperación, verificación,
  páginas legales y de error) DEBE requerir sesión iniciada.

**Recuperación y cambio de contraseña**

- **FR-019**: El sistema DEBE permitir solicitar la recuperación de contraseña indicando un email y
  DEBE enviar, si la cuenta existe, un enlace de un solo uso válido durante 1 hora.
- **FR-020**: Cada nuevo enlace de recuperación DEBE invalidar los anteriores de la misma cuenta.
- **FR-021**: Al completar la recuperación, el sistema DEBE cerrar todas las sesiones de la cuenta,
  levantar un posible bloqueo, marcar la cuenta como verificada y notificar el cambio por email.
- **FR-022**: El sistema DEBE permitir a un usuario autenticado cambiar su contraseña indicando la
  actual; al hacerlo DEBE cerrar sus demás sesiones y notificar el cambio por email.

**Política de contraseñas**

- **FR-023**: Las contraseñas DEBEN tener entre 12 y 128 caracteres y admitir cualquier carácter
  imprimible, incluidos espacios y caracteres no latinos.
- **FR-024**: Por defecto, el sistema NO DEBE imponer reglas de composición (mayúsculas, caracteres
  especiales, etc.) ni caducidad periódica de contraseñas. Los proyectos derivados PUEDEN activar
  por configuración la obligatoriedad de al menos una mayúscula y de al menos un carácter especial
  (cualquier carácter que no sea letra ni número).
- **FR-025**: El sistema DEBE rechazar contraseñas que figuren en una lista de contraseñas comunes o
  filtradas incluida en la propia aplicación, o que contengan el email o el nombre del usuario.
- **FR-026**: El sistema DEBE mostrar los requisitos de la contraseña en los formularios donde se
  establece una.
- **FR-027**: El sistema NUNCA DEBE almacenar, mostrar, enviar por email ni registrar en logs una
  contraseña en claro.

**Privacidad y no enumeración**

- **FR-028**: Las respuestas visibles de registro, inicio de sesión, recuperación y reenvío de
  verificación NO DEBEN permitir deducir si un email tiene cuenta, ni si está bloqueada. La única
  excepción es el aviso de cuenta pendiente de verificación, que solo se muestra cuando se ha
  introducido la contraseña correcta de esa cuenta.
- **FR-029**: El tiempo de respuesta de esas operaciones NO DEBE variar de forma apreciable según
  exista o no la cuenta.
- **FR-030**: Los enlaces enviados por email DEBEN ser imposibles de adivinar y el sistema NO DEBE
  guardarlos de forma que, con acceso a los datos almacenados, puedan reutilizarse.
- **FR-031**: Los registros técnicos (logs) NO DEBEN contener contraseñas, enlaces de un solo uso ni
  más datos personales de los imprescindibles.

**Interfaz y notificaciones**

- **FR-032**: El sistema DEBE ofrecer páginas web de registro, inicio de sesión, solicitud de
  recuperación, establecimiento de nueva contraseña, cambio de contraseña, reenvío de verificación y
  una página de inicio para usuarios autenticados con su nombre, el acceso al cambio de contraseña y
  el cierre de sesión.
- **FR-033**: Los formularios DEBEN rechazar envíos originados en otros sitios web.
- **FR-034**: Las páginas de error DEBEN ser genéricas y no mostrar detalles internos.
- **FR-035**: Los textos de la interfaz y de los emails DEBEN estar en español y ser personalizables
  por los proyectos derivados sin modificar el código de la plantilla.
- **FR-036**: Los parámetros de seguridad de esta feature (longitud mínima de la contraseña,
  obligatoriedad de mayúscula y de carácter especial, límite de peticiones por origen y duración del
  freno, intentos antes del bloqueo, duración del
  bloqueo, validez de los enlaces, inactividad y duración máxima de la sesión, plazo de borrado de cuentas sin
  verificar) DEBEN ser configurables, con los valores de esta especificación como valores por defecto.

### Key Entities *(include if feature involves data)*

- **Usuario**: persona con cuenta. Atributos: nombre, email (único), contraseña protegida, estado
  (pendiente de verificación, activo), fecha de alta, fecha de verificación, número de intentos
  fallidos consecutivos y fin del bloqueo temporal, si lo hay.
- **Consentimiento**: aceptación de un texto legal por un usuario. Atributos: tipo de documento
  (condiciones de uso, política de privacidad), versión aceptada, fecha y hora. Un usuario tiene uno
  o varios consentimientos; se conservan los históricos.
- **Enlace de un solo uso**: credencial temporal asociada a un usuario. Atributos: finalidad
  (verificación de email, recuperación de contraseña), fecha de caducidad, fecha de uso. Se almacena
  de forma que no pueda reconstruirse el enlace original.
- **Sesión**: periodo autenticado de un usuario en un navegador. Un usuario puede tener varias
  sesiones simultáneas sin límite. Se puede invalidar individualmente o todas las de un usuario a la
  vez.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una persona nueva completa el registro y la verificación del email en menos de 3
  minutos, sin contar el tiempo de entrega del email.
- **SC-002**: Una persona que ha olvidado su contraseña recupera el acceso en menos de 5 minutos, sin
  contar el tiempo de entrega del email.
- **SC-003**: En las pruebas de seguridad, el 100% de las respuestas de registro, inicio de sesión,
  recuperación y reenvío son idénticas en contenido para emails con y sin cuenta, y la diferencia
  media de tiempo de respuesta entre ambos casos es inferior a 50 ms.
- **SC-004**: El 0% de los enlaces caducados, ya usados, sustituidos por otros más recientes o
  manipulados permite completar una verificación o un cambio de contraseña.
- **SC-005**: Tras 5 intentos fallidos consecutivos, el 100% de los intentos posteriores contra esa
  cuenta durante los 15 minutos siguientes se rechazan, incluso con la contraseña correcta.
- **SC-006**: Tras un cambio o recuperación de contraseña, el 100% de las sesiones que debían cerrarse
  dejan de dar acceso a páginas privadas.
- **SC-007**: El 100% de las páginas privadas rechazan el acceso sin sesión iniciada.
- **SC-008**: Las páginas de la feature cumplen el nivel AA de las pautas de accesibilidad WCAG 2.1 y
  se usan sin desplazamiento horizontal en pantallas de móvil de 360 px de ancho.
- **SC-009**: El 95% de las páginas de la feature se muestran en menos de 2 segundos con 100 sesiones
  simultáneas activas y 10.000 usuarios registrados, en una sola instancia.
- **SC-010**: Pasar de una a varias instancias de la aplicación no requiere cambios de código, solo
  de configuración, y las sesiones siguen siendo válidas en cualquier instancia.

## Assumptions

- **Escala y despliegue**: por defecto, una sola instancia con hasta 10.000 usuarios registrados y
  100 sesiones simultáneas. Las sesiones se guardan en la memoria del servidor; si un proyecto
  derivado necesita varias instancias, las comparte activándolo por configuración.
- **Usuarios destinatarios**: cualquier persona con acceso a internet y a un buzón de email; no hay
  distinción de tipos de usuario en esta feature (todos tienen el mismo nivel de acceso).
- **Fuera de alcance** (features posteriores): roles y administración de usuarios, menús por rol,
  registro de auditoría de eventos de seguridad, exportación y supresión de datos a petición del
  usuario, segundo factor de autenticación (MFA), inicio de sesión con proveedores externos, cambio
  de email y opción "recordarme" (sesión persistente).
- **Auditoría**: hasta que exista la feature de auditoría, los eventos de seguridad solo dejan rastro
  en los registros técnicos, sin datos personales innecesarios.
- **Valores por defecto**: los plazos y umbrales (24 h para verificar, 1 h para recuperar, 5 intentos
  y 15 minutos de bloqueo, 30 minutos de inactividad y 12 horas de duración máxima de la sesión,
  20 peticiones por minuto y origen, 7 días para borrar cuentas sin verificar) siguen
  prácticas habituales del sector (OWASP ASVS, NIST SP 800-63B) y son configurables.
- **Política de contraseñas**: por defecto sigue OWASP ASVS nivel 2 (mínimo 12 caracteres, sin
  reglas de composición), conforme al principio I de la constitución. Las reglas de mayúscula y
  carácter especial obligatorios existen solo como opción configurable para los proyectos derivados
  que las necesiten (por ejemplo, por exigencias de una política corporativa), desactivadas por
  defecto. Si un proyecto derivado las activa o reduce la longitud mínima, se aparta de ASVS bajo su
  propia responsabilidad.
- **Bloqueo de cuentas**: se acepta el riesgo de que un tercero provoque bloqueos temporales de una
  cuenta ajena; queda mitigado por la duración limitada, la limitación por origen y la posibilidad de
  desbloquear mediante la recuperación de contraseña.
- **Lista de contraseñas prohibidas**: se incluye en la aplicación; no se consulta ningún servicio
  externo, conforme a la constitución.
- **Envío de email**: existe un servidor de correo saliente configurable; en desarrollo se usa un
  servidor de pruebas que no entrega correos reales.
- **Textos legales**: la plantilla incluye textos de ejemplo claramente marcados como tales; cada
  proyecto derivado aporta los suyos, revisados por un profesional competente.
- **Idioma**: la interfaz y los emails están en español en esta feature.
