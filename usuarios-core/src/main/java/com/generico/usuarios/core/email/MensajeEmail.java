package com.generico.usuarios.core.email;

import java.util.Map;

/**
 * Email que hay que enviar: destinatario, nombre de plantilla ({@code templates/usuarios/email/})
 * y variables para la plantilla.
 */
public record MensajeEmail(String destinatario, String plantilla, Map<String, Object> variables) {

    public MensajeEmail {
        variables = Map.copyOf(variables);
    }
}
