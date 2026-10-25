package com.generico.usuarios.core.seguridad;

import java.time.Clock;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.generico.usuarios.core.config.UsuariosProperties;
import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.email.MensajeEmail;
import com.generico.usuarios.core.email.SolicitudEmail;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;

/**
 * Cuenta los fallos y bloquea la cuenta (FR-012, FR-013). Hasta la feature de auditoría, deja
 * rastro de los eventos en los logs solo con el id interno (FR-031).
 */
@Component
public class OyenteEventosAutenticacion {

    private static final Logger LOG = LoggerFactory.getLogger(OyenteEventosAutenticacion.class);

    private final UsuarioRepositorio usuarios;
    private final ApplicationEventPublisher eventos;
    private final UsuariosProperties propiedades;
    private final Clock reloj;

    public OyenteEventosAutenticacion(UsuarioRepositorio usuarios, ApplicationEventPublisher eventos,
            UsuariosProperties propiedades, Clock reloj) {
        this.usuarios = usuarios;
        this.eventos = eventos;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @EventListener
    @Transactional
    public void alFallar(AuthenticationFailureBadCredentialsEvent evento) {
        String email = Usuario.normalizarEmail(evento.getAuthentication().getName());
        usuarios.findParaActualizarByEmail(email).ifPresentOrElse(this::registrarFallo,
                () -> LOG.info("Inicio de sesión fallido con una cuenta inexistente"));
    }

    @EventListener
    @Transactional
    public void alAcceder(AuthenticationSuccessEvent evento) {
        if (evento.getAuthentication().getPrincipal() instanceof UsuarioAutenticado principal) {
            usuarios.findParaActualizarById(principal.getId()).ifPresent(Usuario::registrarExito);
            LOG.info("Inicio de sesión correcto del usuario {}", principal.getId());
        }
    }

    /**
     * Cuenta un fallo de contraseña y, si bloquea la cuenta, avisa al titular. También lo usa el
     * cambio de contraseña (historia 4). Debe llamarse dentro de una transacción.
     */
    public void registrarFallo(Usuario usuario) {
        var bloqueo = propiedades.bloqueo();
        boolean bloqueada = usuario.registrarFallo(reloj.instant(), bloqueo.intentos(), bloqueo.duracion());
        LOG.info("Contraseña incorrecta para el usuario {}", usuario.getId());
        if (bloqueada) {
            LOG.warn("Usuario {} bloqueado {} minutos tras {} intentos fallidos", usuario.getId(),
                    bloqueo.duracion().toMinutes(), bloqueo.intentos());
            eventos.publishEvent(new SolicitudEmail(usuario.getId(), new MensajeEmail(usuario.getEmail(), "cuenta-bloqueada", Map.of(
                    "nombre", usuario.getNombre(),
                    "minutos", bloqueo.duracion().toMinutes(),
                    "urlRecuperacion", propiedades.url("/recuperacion")))));
        }
    }
}
