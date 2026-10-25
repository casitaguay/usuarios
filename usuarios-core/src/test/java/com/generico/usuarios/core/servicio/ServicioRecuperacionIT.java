package com.generico.usuarios.core.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.generico.usuarios.core.contrasena.ContrasenaNoValidaException;
import com.generico.usuarios.core.dominio.EstadoUsuario;
import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.email.NotificadorEmailEnMemoria;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;
import com.generico.usuarios.core.seguridad.ServicioSesiones;
import com.generico.usuarios.core.soporte.BaseIT;

class ServicioRecuperacionIT extends BaseIT {

    private static final String ANTIGUA = "caballo grapa bateria";
    private static final String NUEVA = "otra frase bastante larga";

    @Autowired
    ServicioRecuperacion recuperacion;

    @Autowired
    UsuarioRepositorio usuarios;

    @Autowired
    PasswordEncoder codificador;

    @MockitoBean
    ServicioSesiones sesiones;

    @Test
    void unEmailInexistenteNoEnviaNadaNiFalla() {
        recuperacion.solicitar("nadie@ejemplo.com");

        assertThat(emails.enviados()).isEmpty();
    }

    @Test
    void unEmailExistenteRecibeUnEnlaceDeUnaHoraQueInvalidaLosAnteriores() {
        crear(true);

        String anterior = solicitar();
        String nuevo = solicitar();

        assertThat(emails.enviados("recuperacion").getLast().variables().get("enlace"))
                .asString().startsWith("http://localhost:8080/recuperacion/nueva?t=");
        assertThat(recuperacion.comprobar(anterior)).isFalse();
        assertThat(recuperacion.comprobar(nuevo)).isTrue();

        reloj.avanzar(Duration.ofHours(1).plusSeconds(1));
        assertThat(recuperacion.comprobar(nuevo)).isFalse();
        assertThat(recuperacion.completar(nuevo, NUEVA)).isFalse();
    }

    @Test
    void completarCambiaLaContrasenaCierraLasSesionesYAvisa() {
        Usuario usuario = crear(true);
        String enlace = solicitar();

        assertThat(recuperacion.completar(enlace, NUEVA)).isTrue();

        Usuario actualizado = usuarios.findById(usuario.getId()).orElseThrow();
        assertThat(codificador.matches(NUEVA, actualizado.getHashContrasena())).isTrue();
        assertThat(codificador.matches(ANTIGUA, actualizado.getHashContrasena())).isFalse();
        assertThat(actualizado.getContrasenaCambiadaEn()).isEqualTo(reloj.instant());
        verify(sesiones).expirarTodas(usuario.getId());
        assertThat(emails.enviados("contrasena-cambiada")).singleElement()
                .satisfies(m -> assertThat(m.destinatario()).isEqualTo("ana@ejemplo.com"));

        assertThat(recuperacion.completar(enlace, "una tercera frase larga")).as("enlace ya usado").isFalse();
        assertThat(recuperacion.completar(enlace + "x", NUEVA)).as("enlace manipulado").isFalse();
    }

    @Test
    void completarLevantaElBloqueoYVerificaUnaCuentaPendiente() {
        Usuario usuario = crear(false);
        for (int i = 0; i < 5; i++) {
            usuario.registrarFallo(reloj.instant(), 5, Duration.ofMinutes(15));
        }
        usuarios.save(usuario);

        recuperacion.completar(solicitar(), NUEVA);

        Usuario actualizado = usuarios.findById(usuario.getId()).orElseThrow();
        assertThat(actualizado.estaBloqueada(reloj.instant())).isFalse();
        assertThat(actualizado.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void unaContrasenaQueIncumpleLaPoliticaNoConsumeElEnlace() {
        crear(true);
        String enlace = solicitar();

        assertThatThrownBy(() -> recuperacion.completar(enlace, "corta")).isInstanceOf(ContrasenaNoValidaException.class);
        assertThatThrownBy(() -> recuperacion.completar(enlace, "mi nombre es ana pruebas"))
                .isInstanceOf(ContrasenaNoValidaException.class);

        assertThat(recuperacion.comprobar(enlace)).isTrue();
        verify(sesiones, never()).expirarTodas(any());
    }

    private Usuario crear(boolean verificada) {
        var usuario = new Usuario("Ana Pruebas", "ana@ejemplo.com", codificador.encode(ANTIGUA), reloj.instant());
        if (verificada) {
            usuario.verificar(reloj.instant());
        }
        return usuarios.save(usuario);
    }

    private String solicitar() {
        recuperacion.solicitar(" ANA@ejemplo.com");
        return NotificadorEmailEnMemoria.enlace(emails.enviados("recuperacion").getLast());
    }
}
