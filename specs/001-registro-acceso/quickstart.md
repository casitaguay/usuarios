# Guía de validación: Registro y acceso de usuarios

**Feature**: `001-registro-acceso` | **Plan**: [plan.md](plan.md)

Guía para arrancar la aplicación en local y comprobar de principio a fin que la feature cumple la
[spec](spec.md). Rutas en [contracts/rutas-web.md](contracts/rutas-web.md); configuración en
[contracts/configuracion.md](contracts/configuracion.md).

## Requisitos previos

- Java 25 (`java -version`).
- PostgreSQL 18 en marcha en `localhost:5432`. En Windows, `psql` está en
  `C:\Program Files\PostgreSQL\18\bin\` (no suele estar en el PATH).
- Mailpit para capturar los emails: ejecutable de Windows desde su página de releases en GitHub.
  SMTP en `localhost:1025` e interfaz web en <http://localhost:8025>.
- No hace falta Maven instalado: el repositorio incluye el wrapper `mvnw`.

## Preparación (una sola vez)

1. Crear el usuario y las dos bases de datos (desarrollo y tests) con `psql` como superusuario:

   ```sql
   CREATE ROLE usuarios LOGIN PASSWORD 'cambia-esta-clave';
   CREATE DATABASE usuarios OWNER usuarios;
   CREATE DATABASE usuarios_test OWNER usuarios;
   ```

2. Copiar `usuarios-web/src/main/resources/application-local.example.yml` a
   `application-local.yml` (excluido de git) y poner la contraseña de la base de datos.

3. Arrancar Mailpit (`mailpit.exe`).

## Ejecutar los tests

```bash
./mvnw verify
```

Resultado esperado: compilación correcta y todos los tests en verde, incluidos los de integración
contra `usuarios_test`. Flyway crea el esquema en el arranque de los tests.

## Arrancar la aplicación

```bash
./mvnw -pl usuarios-web spring-boot:run -Dspring-boot.run.profiles=local
```

La aplicación queda en <http://localhost:8080>. Para Tomcat externo: `./mvnw package` y copiar
`usuarios-web/target/usuarios-web.war` a la carpeta `webapps` de Tomcat 11, con las variables de
entorno de [configuracion.md](contracts/configuracion.md).

## Escenarios de validación manual

| # | Escenario | Pasos | Resultado esperado |
|---|---|---|---|
| 1 | Registro y verificación | Registrarse en `/registro`; abrir el email en Mailpit y pulsar el enlace | Cuenta verificada; consentimientos con versión en la tabla `consentimiento` (historia 1) |
| 2 | Registro con email existente | Repetir el registro con el mismo email | Mismo mensaje que en el paso 1; en Mailpit llega el aviso `registro-existente` |
| 3 | Inicio y cierre de sesión | Entrar en `/acceso`, ver `/inicio`, pulsar "Cerrar sesión" e intentar volver a `/inicio` | Redirección a `/acceso` (historia 2) |
| 4 | Sin enumeración | Intentar entrar con un email inexistente y con una contraseña errónea | Mismo mensaje genérico en ambos casos |
| 5 | Bloqueo | 5 intentos fallidos con una cuenta verificada; después, la contraseña correcta | Acceso denegado durante 15 min y email `cuenta-bloqueada` |
| 6 | Recuperación | Pedir recuperación en `/recuperacion`, usar el enlace, poner contraseña nueva | Entra con la nueva, no con la antigua; email `contrasena-cambiada` (historia 3) |
| 7 | Enlace reutilizado | Volver a abrir el enlace del paso 6 | Mensaje de enlace no válido |
| 8 | Cambio de contraseña | Con dos navegadores con sesión, cambiar la contraseña en uno | El otro navegador pierde la sesión (historia 4) |
| 9 | Limitación por origen | Más de 20 envíos de `/acceso` en un minuto | Página "Demasiados intentos" (429) |
| 10 | Cabeceras | Inspeccionar la respuesta en las herramientas del navegador | CSP, `nosniff`, `Referrer-Policy` y cookie `HttpOnly`/`SameSite` presentes |

Los escenarios con tiempos largos (caducidad de enlaces, 12 horas de sesión, borrado a los 7 días)
se validan con los tests automáticos, que usan un reloj controlado.
