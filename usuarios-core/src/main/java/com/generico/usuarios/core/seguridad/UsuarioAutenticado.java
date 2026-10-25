package com.generico.usuarios.core.seguridad;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.userdetails.User;

/**
 * Principal de la sesión: el email como nombre de usuario, más el id interno y el nombre visible.
 * Siempre habilitado y no bloqueado ante las comprobaciones previas (research R5).
 */
public class UsuarioAutenticado extends User {

    private final UUID id;
    private final String nombre;

    public UsuarioAutenticado(UUID id, String email, String nombre, String hashContrasena) {
        super(email, hashContrasena, List.of());
        this.id = id;
        this.nombre = nombre;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
