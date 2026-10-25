package com.generico.usuarios.core.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía los emails de forma asíncrona y después del commit, para que la respuesta no dependa de
 * si la cuenta existe ni del servidor de correo (research R8).
 */
@Component
public class OyenteSolicitudEmail {

    private static final Logger LOG = LoggerFactory.getLogger(OyenteSolicitudEmail.class);

    private final NotificadorEmail notificador;

    public OyenteSolicitudEmail(NotificadorEmail notificador) {
        this.notificador = notificador;
    }

    @Async("ejecutorEmail")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alSolicitar(SolicitudEmail solicitud) {
        try {
            notificador.enviar(solicitud.mensaje());
        } catch (RuntimeException e) {
            // Solo el tipo de excepción: su mensaje puede incluir la dirección de email (FR-031)
            LOG.warn("No se pudo enviar el email '{}' al usuario {}: {}",
                    solicitud.mensaje().plantilla(), solicitud.usuarioId(), e.getClass().getName());
        }
    }
}
