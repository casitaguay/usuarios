package com.generico.usuarios.core.seguridad;

import java.time.Clock;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Component;

import com.generico.usuarios.core.dominio.EstadoUsuario;
import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;

/**
 * Se ejecuta solo con la contraseña ya validada (research R5): así ni la respuesta ni el tiempo
 * revelan si una cuenta existe o está bloqueada. Una cuenta bloqueada responde como credenciales incorrectas.
 */
@Component
public class ComprobacionesPosteriores implements UserDetailsChecker {

    private final UsuarioRepositorio usuarios;
    private final Clock reloj;

    public ComprobacionesPosteriores(UsuarioRepositorio usuarios, Clock reloj) {
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    @Override
    public void check(UserDetails detalles) {
        Usuario usuario = usuarios.findById(((UsuarioAutenticado) detalles).getId())
                .orElseThrow(() -> new BadCredentialsException("Cuenta inexistente"));
        if (usuario.estaBloqueada(reloj.instant())) {
            throw new BadCredentialsException("Cuenta bloqueada");
        }
        if (usuario.getEstado() == EstadoUsuario.PENDIENTE_VERIFICACION) {
            throw new CuentaPendienteException();
        }
    }
}
