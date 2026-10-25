package com.generico.usuarios.core.servicio;

import java.time.Clock;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.generico.usuarios.core.config.UsuariosProperties;
import com.generico.usuarios.core.dominio.EstadoUsuario;
import com.generico.usuarios.core.dominio.FinalidadEnlace;
import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.email.MensajeEmail;
import com.generico.usuarios.core.email.SolicitudEmail;
import com.generico.usuarios.core.enlace.ServicioEnlaces;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;

/** Verificación del email y reenvío del enlace (FR-007 a FR-009). */
@Service
public class ServicioVerificacion {

    public enum ResultadoVerificacion {
        VERIFICADA,
        YA_VERIFICADA,
        NO_VALIDO
    }

    private final UsuarioRepositorio usuarios;
    private final ServicioEnlaces enlaces;
    private final ApplicationEventPublisher eventos;
    private final UsuariosProperties propiedades;
    private final Clock reloj;

    public ServicioVerificacion(UsuarioRepositorio usuarios, ServicioEnlaces enlaces, ApplicationEventPublisher eventos,
            UsuariosProperties propiedades, Clock reloj) {
        this.usuarios = usuarios;
        this.enlaces = enlaces;
        this.eventos = eventos;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Transactional
    public ResultadoVerificacion verificar(String valor) {
        return enlaces.consumir(valor, FinalidadEnlace.VERIFICACION_EMAIL)
                .map(usuario -> {
                    if (usuario.getEstado() == EstadoUsuario.ACTIVO) {
                        return ResultadoVerificacion.YA_VERIFICADA;
                    }
                    usuario.verificar(reloj.instant());
                    return ResultadoVerificacion.VERIFICADA;
                })
                .orElse(ResultadoVerificacion.NO_VALIDO);
    }

    /** Envía un enlace nuevo si la cuenta existe y está pendiente; si no, no hace nada ni lo indica (FR-028). */
    @Transactional
    public void reenviar(String email) {
        usuarios.findByEmail(Usuario.normalizarEmail(email))
                .filter(usuario -> usuario.getEstado() == EstadoUsuario.PENDIENTE_VERIFICACION)
                .ifPresent(this::enviarEnlace);
    }

    /** Emite un enlace de verificación (invalidando los anteriores) y pide su envío por email. */
    @Transactional
    public void enviarEnlace(Usuario usuario) {
        var validez = propiedades.verificacion().validez();
        String valor = enlaces.emitir(usuario, FinalidadEnlace.VERIFICACION_EMAIL, validez);
        eventos.publishEvent(new SolicitudEmail(usuario.getId(), new MensajeEmail(usuario.getEmail(), "verificacion", Map.of(
                "nombre", usuario.getNombre(),
                "enlace", propiedades.url("/verificacion?t=" + valor),
                "horasValidez", validez.toHours()))));
    }
}
