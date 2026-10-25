package com.generico.usuarios.core.seguridad;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;

/**
 * Carga la cuenta por email normalizado. Siempre la presenta habilitada y sin bloquear: el bloqueo
 * y la verificación se comprueban después de la contraseña, en {@link ComprobacionesPosteriores} (research R5).
 */
@Service
public class ServicioDetallesUsuario implements UserDetailsService {

    private final UsuarioRepositorio usuarios;

    public ServicioDetallesUsuario(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return usuarios.findByEmail(Usuario.normalizarEmail(email))
                .map(u -> new UsuarioAutenticado(u.getId(), u.getEmail(), u.getNombre(), u.getHashContrasena()))
                .orElseThrow(() -> new UsernameNotFoundException("Cuenta inexistente"));
    }
}
