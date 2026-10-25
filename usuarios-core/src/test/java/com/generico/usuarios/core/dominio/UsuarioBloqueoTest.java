package com.generico.usuarios.core.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class UsuarioBloqueoTest {

    private static final Instant AHORA = Instant.parse("2026-01-01T10:00:00Z");
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private final Usuario usuario = new Usuario("Ana", "ana@ejemplo.com", "{argon2}hash", AHORA);

    @Test
    void elQuintoFalloBloqueaQuinceMinutosYReiniciaElContador() {
        for (int i = 1; i <= 4; i++) {
            assertThat(usuario.registrarFallo(AHORA, 5, BLOQUEO)).isFalse();
        }
        assertThat(usuario.registrarFallo(AHORA, 5, BLOQUEO)).isTrue();

        assertThat(usuario.getBloqueadoHasta()).isEqualTo(AHORA.plus(BLOQUEO));
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.estaBloqueada(AHORA)).isTrue();
    }

    @Test
    void unFalloDuranteElBloqueoNoLoAmplia() {
        bloquear();
        Instant despues = AHORA.plusSeconds(60);

        assertThat(usuario.registrarFallo(despues, 5, BLOQUEO)).isFalse();

        assertThat(usuario.getBloqueadoHasta()).isEqualTo(AHORA.plus(BLOQUEO));
        assertThat(usuario.getIntentosFallidos()).isZero();
    }

    @Test
    void registrarExitoReiniciaContadorYBloqueo() {
        usuario.registrarFallo(AHORA, 5, BLOQUEO);
        bloquear();

        usuario.registrarExito();

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isNull();
        assertThat(usuario.estaBloqueada(AHORA)).isFalse();
    }

    @Test
    void elBloqueoTerminaPasadoElPlazo() {
        bloquear();

        assertThat(usuario.estaBloqueada(AHORA.plus(BLOQUEO).minusSeconds(1))).isTrue();
        assertThat(usuario.estaBloqueada(AHORA.plus(BLOQUEO))).isFalse();
    }

    @Test
    void elEmailSeNormaliza() {
        Usuario u = new Usuario("Ana", "  Ana@Ejemplo.COM ", "{argon2}hash", AHORA);
        assertThat(u.getEmail()).isEqualTo("ana@ejemplo.com");

        u.setEmail(" OTRA@Ejemplo.com");
        assertThat(u.getEmail()).isEqualTo("otra@ejemplo.com");
    }

    private void bloquear() {
        for (int i = 0; i < 5; i++) {
            usuario.registrarFallo(AHORA, 5, BLOQUEO);
        }
    }
}
