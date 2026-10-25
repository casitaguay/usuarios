-- Feature 001: registro y acceso (data-model.md). El core usa V1–V999; los derivados, V1000 en adelante.

CREATE TABLE usuario (
    id                     uuid         PRIMARY KEY,
    nombre                 varchar(100) NOT NULL,
    email                  varchar(254) NOT NULL UNIQUE,
    hash_contrasena        varchar(255) NOT NULL,
    estado                 varchar(30)  NOT NULL
        CHECK (estado IN ('PENDIENTE_VERIFICACION', 'ACTIVO')),
    creado_en              timestamptz  NOT NULL,
    verificado_en          timestamptz,
    intentos_fallidos      integer      NOT NULL DEFAULT 0,
    bloqueado_hasta        timestamptz,
    contrasena_cambiada_en timestamptz  NOT NULL,
    version                integer      NOT NULL
);

CREATE INDEX usuario_estado_creado_en_idx ON usuario (estado, creado_en);

CREATE TABLE consentimiento (
    id                uuid        PRIMARY KEY,
    usuario_id        uuid        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tipo_documento    varchar(30) NOT NULL
        CHECK (tipo_documento IN ('CONDICIONES_USO', 'POLITICA_PRIVACIDAD')),
    version_documento varchar(30) NOT NULL,
    aceptado_en       timestamptz NOT NULL
);

CREATE TABLE enlace_un_uso (
    id            uuid        PRIMARY KEY,
    usuario_id    uuid        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    finalidad     varchar(30) NOT NULL
        CHECK (finalidad IN ('VERIFICACION_EMAIL', 'RECUPERACION_CONTRASENA')),
    hash_valor    char(64)    NOT NULL UNIQUE,
    creado_en     timestamptz NOT NULL,
    caduca_en     timestamptz NOT NULL,
    usado_en      timestamptz,
    invalidado_en timestamptz
);

CREATE INDEX enlace_un_uso_usuario_finalidad_idx ON enlace_un_uso (usuario_id, finalidad);
