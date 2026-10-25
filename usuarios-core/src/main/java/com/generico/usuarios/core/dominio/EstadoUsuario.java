package com.generico.usuarios.core.dominio;

/** Estado de la cuenta (data-model.md). El bloqueo temporal no es un estado; se deriva de {@code bloqueadoHasta}. */
public enum EstadoUsuario {
    PENDIENTE_VERIFICACION,
    ACTIVO
}
