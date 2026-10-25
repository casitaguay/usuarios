package com.generico.usuarios.core.servicio;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.generico.usuarios.core.config.UsuariosProperties;
import com.generico.usuarios.core.contrasena.ContrasenaNoValidaException;
import com.generico.usuarios.core.contrasena.PoliticaContrasenas;
import com.generico.usuarios.core.dominio.Consentimiento;
import com.generico.usuarios.core.dominio.TipoDocumento;
import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.email.MensajeEmail;
import com.generico.usuarios.core.email.SolicitudEmail;
import com.generico.usuarios.core.repositorio.ConsentimientoRepositorio;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;

/**
 * Registro con consentimiento (historia 1). No indica nunca si el email ya tenía cuenta: en ese
 * caso avisa al titular por email y responde igual (FR-028).
 */
@Service
public class ServicioRegistro {

    private final UsuarioRepositorio usuarios;
    private final ConsentimientoRepositorio consentimientos;
    private final ServicioVerificacion verificacion;
    private final PoliticaContrasenas politica;
    private final PasswordEncoder codificador;
    private final ApplicationEventPublisher eventos;
    private final UsuariosProperties propiedades;
    private final Clock reloj;

    public ServicioRegistro(UsuarioRepositorio usuarios, ConsentimientoRepositorio consentimientos,
            ServicioVerificacion verificacion, PoliticaContrasenas politica, PasswordEncoder codificador,
            ApplicationEventPublisher eventos, UsuariosProperties propiedades, Clock reloj) {
        this.usuarios = usuarios;
        this.consentimientos = consentimientos;
        this.verificacion = verificacion;
        this.politica = politica;
        this.codificador = codificador;
        this.eventos = eventos;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    /** @throws ContrasenaNoValidaException si la contraseña incumple la política */
    @Transactional
    public void registrar(String nombre, String email, String contrasena) {
        List<String> incumplidos = politica.validar(contrasena, email, nombre);
        if (!incumplidos.isEmpty()) {
            throw new ContrasenaNoValidaException(incumplidos);
        }
        // Se calcula siempre, también si el email existe, para que el tiempo de respuesta no lo delate (FR-029)
        String hash = codificador.encode(contrasena);
        String normalizado = Usuario.normalizarEmail(email);

        usuarios.findByEmail(normalizado).ifPresentOrElse(
                existente -> avisarRegistroExistente(existente),
                () -> crearCuenta(nombre, normalizado, hash));
    }

    private void crearCuenta(String nombre, String email, String hash) {
        Instant ahora = reloj.instant();
        Usuario usuario = usuarios.save(new Usuario(nombre, email, hash, ahora));
        var legal = propiedades.legal();
        consentimientos.save(new Consentimiento(usuario, TipoDocumento.CONDICIONES_USO, legal.condiciones().version(), ahora));
        consentimientos.save(new Consentimiento(usuario, TipoDocumento.POLITICA_PRIVACIDAD, legal.privacidad().version(), ahora));
        verificacion.enviarEnlace(usuario);
    }

    private void avisarRegistroExistente(Usuario existente) {
        eventos.publishEvent(new SolicitudEmail(existente.getId(), new MensajeEmail(existente.getEmail(), "registro-existente", Map.of(
                "nombre", existente.getNombre(),
                "urlRecuperacion", propiedades.url("/recuperacion")))));
    }
}
