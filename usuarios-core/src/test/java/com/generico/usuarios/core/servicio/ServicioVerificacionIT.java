package com.generico.usuarios.core.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.generico.usuarios.core.dominio.EstadoUsuario;
import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.email.NotificadorEmailEnMemoria;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;
import com.generico.usuarios.core.servicio.ServicioVerificacion.ResultadoVerificacion;
import com.generico.usuarios.core.soporte.BaseIT;

class ServicioVerificacionIT extends BaseIT {

    @Autowired
    ServicioRegistro registro;

    @Autowired
    ServicioVerificacion verificacion;

    @Autowired
    UsuarioRepositorio usuarios;

    @Test
    void unEnlaceValidoActivaLaCuenta() {
        String enlace = registrar();

        assertThat(verificacion.verificar(enlace)).isEqualTo(ResultadoVerificacion.VERIFICADA);

        Usuario usuario = usuarios.findByEmail("ana@ejemplo.com").orElseThrow();
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(usuario.getVerificadoEn()).isEqualTo(reloj.instant());
    }

    @Test
    void unEnlaceValidoDeUnaCuentaYaVerificadaLoIndica() {
        String enlace = registrar();
        Usuario usuario = usuarios.findByEmail("ana@ejemplo.com").orElseThrow();
        usuario.verificar(reloj.instant());
        usuarios.save(usuario);

        assertThat(verificacion.verificar(enlace)).isEqualTo(ResultadoVerificacion.YA_VERIFICADA);
    }

    @Test
    void caducadoUsadoOManipuladoNoEsValido() {
        String enlace = registrar();

        assertThat(verificacion.verificar(enlace + "x")).isEqualTo(ResultadoVerificacion.NO_VALIDO);
        assertThat(verificacion.verificar(null)).isEqualTo(ResultadoVerificacion.NO_VALIDO);

        reloj.avanzar(Duration.ofHours(24).plusSeconds(1));
        assertThat(verificacion.verificar(enlace)).isEqualTo(ResultadoVerificacion.NO_VALIDO);

        reloj.reiniciar();
        String otro = reenviar();
        assertThat(verificacion.verificar(otro)).isEqualTo(ResultadoVerificacion.VERIFICADA);
        assertThat(verificacion.verificar(otro)).isEqualTo(ResultadoVerificacion.NO_VALIDO);
    }

    @Test
    void elReenvioInvalidaLosEnlacesAnteriores() {
        String anterior = registrar();
        String nuevo = reenviar();

        assertThat(verificacion.verificar(anterior)).isEqualTo(ResultadoVerificacion.NO_VALIDO);
        assertThat(verificacion.verificar(nuevo)).isEqualTo(ResultadoVerificacion.VERIFICADA);
    }

    @Test
    void elReenvioNoHaceNadaSiElEmailNoExisteOYaEstaVerificado() {
        verificacion.reenviar("nadie@ejemplo.com");
        assertThat(emails.enviados()).isEmpty();

        verificacion.verificar(registrar());
        emails.limpiar();
        verificacion.reenviar("ana@ejemplo.com");
        assertThat(emails.enviados()).isEmpty();
    }

    private String registrar() {
        registro.registrar("Ana", "ana@ejemplo.com", ServicioRegistroIT.CONTRASENA);
        return ultimoEnlace();
    }

    private String reenviar() {
        verificacion.reenviar(" ANA@ejemplo.com");
        return ultimoEnlace();
    }

    private String ultimoEnlace() {
        var enviados = emails.enviados("verificacion");
        return NotificadorEmailEnMemoria.enlace(enviados.getLast());
    }
}
