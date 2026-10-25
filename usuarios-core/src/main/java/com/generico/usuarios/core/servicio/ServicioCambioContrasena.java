package com.generico.usuarios.core.servicio;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.generico.usuarios.core.contrasena.ContrasenaNoValidaException;
import com.generico.usuarios.core.contrasena.PoliticaContrasenas;
import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;
import com.generico.usuarios.core.seguridad.OyenteEventosAutenticacion;
import com.generico.usuarios.core.seguridad.ServicioSesiones;

/** Cambio de contraseña con sesión iniciada (historia 4, FR-022). */
@Service
public class ServicioCambioContrasena {

    /** Clave de mensaje: la contraseña nueva es igual a la actual. */
    public static final String IGUAL_A_LA_ACTUAL = "contrasena.igual-actual";

    private final UsuarioRepositorio usuarios;
    private final PoliticaContrasenas politica;
    private final PasswordEncoder codificador;
    private final ServicioSesiones sesiones;
    private final OyenteEventosAutenticacion fallos;
    private final ServicioRecuperacion recuperacion;
    private final Clock reloj;

    public ServicioCambioContrasena(UsuarioRepositorio usuarios, PoliticaContrasenas politica, PasswordEncoder codificador,
            ServicioSesiones sesiones, OyenteEventosAutenticacion fallos, ServicioRecuperacion recuperacion, Clock reloj) {
        this.usuarios = usuarios;
        this.politica = politica;
        this.codificador = codificador;
        this.sesiones = sesiones;
        this.fallos = fallos;
        this.recuperacion = recuperacion;
        this.reloj = reloj;
    }

    /**
     * Cambia la contraseña y cierra las demás sesiones de la cuenta; la actual sigue abierta.
     *
     * @throws ContrasenaActualIncorrectaException si {@code actual} no es correcta (cuenta como fallo y puede bloquear la cuenta)
     * @throws ContrasenaNoValidaException si la nueva incumple la política o es igual a la actual
     */
    // El fallo contado debe guardarse aunque se lance la excepción
    @Transactional(noRollbackFor = ContrasenaActualIncorrectaException.class)
    public void cambiar(UUID usuarioId, String actual, String nueva, String idSesionActual) {
        Usuario usuario = usuarios.findParaActualizarById(usuarioId).orElseThrow();
        if (!codificador.matches(actual, usuario.getHashContrasena())) {
            fallos.registrarFallo(usuario);
            throw new ContrasenaActualIncorrectaException();
        }
        List<String> incumplidos = new ArrayList<>(politica.validar(nueva, usuario.getEmail(), usuario.getNombre()));
        if (codificador.matches(nueva, usuario.getHashContrasena())) {
            incumplidos.add(IGUAL_A_LA_ACTUAL);
        }
        if (!incumplidos.isEmpty()) {
            throw new ContrasenaNoValidaException(incumplidos);
        }
        Instant ahora = reloj.instant();
        usuario.cambiarHashContrasena(codificador.encode(nueva), ahora);
        sesiones.expirarTodasSalvo(usuarioId, idSesionActual);
        recuperacion.avisarContrasenaCambiada(usuario, ahora);
    }
}
