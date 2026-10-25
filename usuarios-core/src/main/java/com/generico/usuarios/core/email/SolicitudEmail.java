package com.generico.usuarios.core.email;

import java.util.UUID;

/** Evento de aplicación: se envía tras el commit de la transacción que lo publica (research R8). */
public record SolicitudEmail(UUID usuarioId, MensajeEmail mensaje) {
}
