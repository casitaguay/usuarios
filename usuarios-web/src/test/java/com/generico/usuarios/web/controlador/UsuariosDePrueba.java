package com.generico.usuarios.web.controlador;

import java.time.Clock;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;

/** Crea cuentas directamente en la base de datos para los tests web. */
@Component
class UsuariosDePrueba {

    static final String CONTRASENA = "caballo grapa bateria";

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;
    private final Clock reloj;

    UsuariosDePrueba(UsuarioRepositorio usuarios, PasswordEncoder codificador, Clock reloj) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.reloj = reloj;
    }

    Usuario activo(String nombre, String email) {
        var usuario = new Usuario(nombre, email, codificador.encode(CONTRASENA), reloj.instant());
        usuario.verificar(reloj.instant());
        return usuarios.save(usuario);
    }

    Usuario pendiente(String nombre, String email) {
        return usuarios.save(new Usuario(nombre, email, codificador.encode(CONTRASENA), reloj.instant()));
    }
}
