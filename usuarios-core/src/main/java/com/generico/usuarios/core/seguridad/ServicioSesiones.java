package com.generico.usuarios.core.seguridad;

import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

/** Cierra las sesiones de un usuario tras recuperar o cambiar la contraseña (FR-021, FR-022). */
@Service
public class ServicioSesiones {

    private final SessionRegistry registro;

    public ServicioSesiones(SessionRegistry registro) {
        this.registro = registro;
    }

    public void expirarTodas(UUID usuarioId) {
        sesiones(usuarioId).forEach(SessionInformation::expireNow);
    }

    public void expirarTodasSalvo(UUID usuarioId, String idSesionActual) {
        sesiones(usuarioId)
                .filter(sesion -> !sesion.getSessionId().equals(idSesionActual))
                .forEach(SessionInformation::expireNow);
    }

    private Stream<SessionInformation> sesiones(UUID usuarioId) {
        return registro.getAllPrincipals().stream()
                .filter(principal -> principal instanceof UsuarioAutenticado u && u.getId().equals(usuarioId))
                .flatMap(principal -> registro.getAllSessions(principal, false).stream());
    }
}
