package com.generico.usuarios.core.servicio;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.generico.usuarios.core.config.UsuariosProperties;
import com.generico.usuarios.core.contrasena.ContrasenaNoValidaException;
import com.generico.usuarios.core.contrasena.PoliticaContrasenas;
import com.generico.usuarios.core.dominio.FinalidadEnlace;
import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.email.MensajeEmail;
import com.generico.usuarios.core.email.SolicitudEmail;
import com.generico.usuarios.core.enlace.ServicioEnlaces;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;
import com.generico.usuarios.core.seguridad.ServicioSesiones;

/** Recuperación de una contraseña olvidada (historia 3, FR-019 a FR-021). */
@Service
public class ServicioRecuperacion {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC);

    private final UsuarioRepositorio usuarios;
    private final ServicioEnlaces enlaces;
    private final PoliticaContrasenas politica;
    private final PasswordEncoder codificador;
    private final ServicioSesiones sesiones;
    private final ApplicationEventPublisher eventos;
    private final UsuariosProperties propiedades;
    private final Clock reloj;

    public ServicioRecuperacion(UsuarioRepositorio usuarios, ServicioEnlaces enlaces, PoliticaContrasenas politica,
            PasswordEncoder codificador, ServicioSesiones sesiones, ApplicationEventPublisher eventos,
            UsuariosProperties propiedades, Clock reloj) {
        this.usuarios = usuarios;
        this.enlaces = enlaces;
        this.politica = politica;
        this.codificador = codificador;
        this.sesiones = sesiones;
        this.eventos = eventos;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    /** Envía un enlace si la cuenta existe; si no, no hace nada ni lo indica (FR-028). */
    @Transactional
    public void solicitar(String email) {
        usuarios.findByEmail(Usuario.normalizarEmail(email)).ifPresent(usuario -> {
            var validez = propiedades.recuperacion().validez();
            String valor = enlaces.emitir(usuario, FinalidadEnlace.RECUPERACION_CONTRASENA, validez);
            eventos.publishEvent(new SolicitudEmail(usuario.getId(), new MensajeEmail(usuario.getEmail(), "recuperacion", Map.of(
                    "nombre", usuario.getNombre(),
                    "enlace", propiedades.url("/recuperacion/nueva?t=" + valor),
                    "minutosValidez", validez.toMinutes()))));
        });
    }

    public boolean comprobar(String valor) {
        return enlaces.titular(valor, FinalidadEnlace.RECUPERACION_CONTRASENA).isPresent();
    }

    /**
     * Establece la contraseña nueva, levanta el bloqueo, verifica la cuenta y cierra todas sus sesiones.
     *
     * @return {@code false} si el enlace no es válido
     * @throws ContrasenaNoValidaException si la contraseña incumple la política (el enlace sigue valiendo)
     */
    @Transactional
    public boolean completar(String valor, String nuevaContrasena) {
        var titular = enlaces.titular(valor, FinalidadEnlace.RECUPERACION_CONTRASENA);
        if (titular.isEmpty()) {
            return false;
        }
        Usuario usuario = titular.get();
        List<String> incumplidos = politica.validar(nuevaContrasena, usuario.getEmail(), usuario.getNombre());
        if (!incumplidos.isEmpty()) {
            throw new ContrasenaNoValidaException(incumplidos);
        }
        // Consumo atómico: de dos envíos simultáneos con el mismo enlace solo uno llega aquí
        if (enlaces.consumir(valor, FinalidadEnlace.RECUPERACION_CONTRASENA).isEmpty()) {
            return false;
        }
        Instant ahora = reloj.instant();
        usuario.cambiarHashContrasena(codificador.encode(nuevaContrasena), ahora);
        usuario.registrarExito();
        usuario.verificar(ahora);
        sesiones.expirarTodas(usuario.getId());
        avisarContrasenaCambiada(usuario, ahora);
        return true;
    }

    /** Email {@code contrasena-cambiada}; también lo usa el cambio de contraseña con sesión (historia 4). */
    public void avisarContrasenaCambiada(Usuario usuario, Instant cuando) {
        eventos.publishEvent(new SolicitudEmail(usuario.getId(), new MensajeEmail(usuario.getEmail(), "contrasena-cambiada", Map.of(
                "nombre", usuario.getNombre(),
                "fechaHora", FECHA_HORA.format(cuando),
                "urlRecuperacion", propiedades.url("/recuperacion")))));
    }
}
