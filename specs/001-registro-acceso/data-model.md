# Modelo de datos: Registro y acceso de usuarios

**Feature**: `001-registro-acceso` | **Fecha**: 2026-10-25 | **Plan**: [plan.md](plan.md)

Base de datos PostgreSQL; esquema gestionado con Flyway (`V1__usuarios.sql` y siguientes en
`usuarios-core`). Nombres de tablas y columnas en español y `snake_case`. Todas las fechas en
`timestamptz` (UTC).

## Usuario (`usuario`)

| Campo | Tipo | Restricciones | Origen |
|---|---|---|---|
| `id` | `uuid` | PK, generado por la aplicación | — |
| `nombre` | `varchar(100)` | no nulo, 1–100 caracteres tras recortar espacios | FR-001 |
| `email` | `varchar(254)` | no nulo, único, guardado normalizado (recortado y en minúsculas) | FR-005 |
| `hash_contrasena` | `varchar(255)` | no nulo, con prefijo de algoritmo (`{argon2}...`) | FR-027, R4 |
| `estado` | `varchar(30)` | no nulo; `PENDIENTE_VERIFICACION` o `ACTIVO` | FR-008 |
| `creado_en` | `timestamptz` | no nulo | FR-010 |
| `verificado_en` | `timestamptz` | nulo hasta verificar | — |
| `intentos_fallidos` | `integer` | no nulo, por defecto 0 | FR-012, FR-013 |
| `bloqueado_hasta` | `timestamptz` | nulo si no está bloqueado | FR-012 |
| `contrasena_cambiada_en` | `timestamptz` | no nulo | FR-021, FR-022 |
| `version` | `integer` | no nulo; bloqueo optimista | — |

Índices: único sobre `email`; índice sobre `(estado, creado_en)` para el borrado de cuentas
pendientes.

Extensibilidad (principio II): la entidad no se declara `final` y los proyectos derivados añaden
sus propios datos en tablas propias relacionadas por `usuario_id`, con sus propias migraciones
(rango de versiones de Flyway reservado: el core usa `V1`–`V999`, los derivados a partir de
`V1000`).

### Estados y transiciones

```text
               registro
                  │
                  ▼
     ┌──────────────────────────┐   7 días sin verificar    ┌──────────┐
     │  PENDIENTE_VERIFICACION  │ ────────────────────────► │ (borrado) │
     └──────────────────────────┘                           └──────────┘
                  │  enlace de verificación válido
                  │  o recuperación de contraseña completada
                  ▼
           ┌─────────────┐
           │   ACTIVO    │  ◄── el bloqueo temporal no es un estado: se deriva de
           └─────────────┘      bloqueado_hasta > ahora
```

- **Bloqueo temporal**: al llegar `intentos_fallidos` a 5, `bloqueado_hasta = ahora + 15 min` y
  `intentos_fallidos = 0`. Un inicio de sesión correcto o una recuperación completada ponen
  `intentos_fallidos = 0` y `bloqueado_hasta = null`.
- Un intento fallido mientras la cuenta está bloqueada no amplía el bloqueo.
- Una contraseña actual incorrecta en el cambio de contraseña cuenta como intento fallido (spec,
  historia 4, escenario 2).

## Consentimiento (`consentimiento`)

| Campo | Tipo | Restricciones | Origen |
|---|---|---|---|
| `id` | `uuid` | PK | — |
| `usuario_id` | `uuid` | FK → `usuario.id`, `ON DELETE CASCADE`, no nulo | — |
| `tipo_documento` | `varchar(30)` | no nulo; `CONDICIONES_USO` o `POLITICA_PRIVACIDAD` | FR-002 |
| `version_documento` | `varchar(30)` | no nulo; versión vigente configurada al aceptar | FR-003 |
| `aceptado_en` | `timestamptz` | no nulo | FR-003 |

Solo inserción: no se modifican ni borran salvo por el borrado en cascada de la cuenta. Un registro
genera dos filas (una por documento).

## Enlace de un solo uso (`enlace_un_uso`)

| Campo | Tipo | Restricciones | Origen |
|---|---|---|---|
| `id` | `uuid` | PK | — |
| `usuario_id` | `uuid` | FK → `usuario.id`, `ON DELETE CASCADE`, no nulo | — |
| `finalidad` | `varchar(30)` | no nulo; `VERIFICACION_EMAIL` o `RECUPERACION_CONTRASENA` | — |
| `hash_valor` | `char(64)` | no nulo, único; SHA-256 en hexadecimal del valor enviado | FR-030, R7 |
| `creado_en` | `timestamptz` | no nulo | — |
| `caduca_en` | `timestamptz` | no nulo; +24 h verificación, +1 h recuperación | FR-007, FR-019 |
| `usado_en` | `timestamptz` | nulo hasta consumirse | — |
| `invalidado_en` | `timestamptz` | nulo salvo que lo sustituya otro más reciente | FR-009, FR-020 |

Un enlace es **válido** si `usado_en IS NULL AND invalidado_en IS NULL AND caduca_en > ahora`.
Índice sobre `(usuario_id, finalidad)`. Los enlaces usados, invalidados o caducados se eliminan en
la misma tarea diaria de limpieza tras 7 días.

## Sesión (no persistida en esta feature)

Sesión HTTP en memoria del servidor (R6). Atributos relevantes: identificador de usuario, hora de
inicio (para la duración máxima de 12 horas) y última actividad (inactividad de 30 minutos). El
registro de sesiones permite invalidar todas las de un usuario o todas menos la actual.

## Reglas de validación (resumen)

| Regla | Dónde | Requisito |
|---|---|---|
| Nombre de 1 a 100 caracteres | Formulario de registro | FR-001 |
| Email con formato válido y ≤ 254 caracteres | Registro, inicio de sesión, recuperación | FR-005 |
| Contraseña de 12 a 128 caracteres | Registro, nueva contraseña, cambio | FR-023 |
| Contraseña fuera de la lista de prohibidas, sin email ni nombre | Ídem | FR-025 |
| Mayúscula y carácter especial (solo si está activado) | Ídem | FR-024 |
| Nueva contraseña distinta de la actual | Cambio de contraseña | Historia 4 |
| Casillas de condiciones y privacidad marcadas | Registro | FR-002 |
