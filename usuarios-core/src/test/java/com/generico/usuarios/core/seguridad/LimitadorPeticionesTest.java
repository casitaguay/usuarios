package com.generico.usuarios.core.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.generico.usuarios.core.config.UsuariosProperties;
import com.generico.usuarios.core.soporte.RelojControlado;

class LimitadorPeticionesTest {

    private static final String IP = "203.0.113.10";
    private static final String GRUPO = "/acceso";

    private final RelojControlado reloj = new RelojControlado();
    private final LimitadorPeticiones limitador = new LimitadorPeticiones(reloj,
            new UsuariosProperties.LimitePeticiones(20, Duration.ofMinutes(5), List.of("10.0.0.1")));

    @Test
    void permiteVeintePorMinutoYRechazaLaSiguiente() {
        for (int i = 0; i < 20; i++) {
            assertThat(limitador.permitir(IP, GRUPO)).isTrue();
        }
        assertThat(limitador.permitir(IP, GRUPO)).isFalse();
    }

    @Test
    void elFrenoDuraCincoMinutos() {
        agotar(IP);

        reloj.avanzar(Duration.ofMinutes(4));
        assertThat(limitador.permitir(IP, GRUPO)).isFalse();

        reloj.avanzar(Duration.ofMinutes(1));
        assertThat(limitador.permitir(IP, GRUPO)).isTrue();
    }

    @Test
    void otraIpUOtroGrupoNoSeVenAfectados() {
        agotar(IP);

        assertThat(limitador.permitir("203.0.113.11", GRUPO)).isTrue();
        assertThat(limitador.permitir(IP, "/registro")).isTrue();
    }

    @Test
    void laVentanaSeReiniciaAlMinuto() {
        for (int i = 0; i < 20; i++) {
            limitador.permitir(IP, GRUPO);
        }
        reloj.avanzar(Duration.ofMinutes(1));

        assertThat(limitador.permitir(IP, GRUPO)).isTrue();
    }

    @Test
    void xForwardedForSoloSeUsaDesdeUnProxyDeConfianza() {
        var directa = peticion("198.51.100.7", "1.2.3.4");
        assertThat(limitador.origen(directa)).isEqualTo("198.51.100.7");

        var desdeProxy = peticion("10.0.0.1", "1.2.3.4, 5.6.7.8");
        assertThat(limitador.origen(desdeProxy)).isEqualTo("5.6.7.8");

        var proxySinCabecera = peticion("10.0.0.1", null);
        assertThat(limitador.origen(proxySinCabecera)).isEqualTo("10.0.0.1");
    }

    @Test
    void laLimpiezaQuitaLasEntradasCaducadas() {
        agotar(IP);
        reloj.avanzar(Duration.ofMinutes(6));

        limitador.limpiar();

        assertThat(limitador.entradas()).isZero();
    }

    private void agotar(String ip) {
        for (int i = 0; i < 21; i++) {
            limitador.permitir(ip, GRUPO);
        }
    }

    private static MockHttpServletRequest peticion(String remota, String reenviadaPara) {
        var peticion = new MockHttpServletRequest();
        peticion.setRemoteAddr(remota);
        if (reenviadaPara != null) {
            peticion.addHeader("X-Forwarded-For", reenviadaPara);
        }
        return peticion;
    }
}
