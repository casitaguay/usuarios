package com.generico.usuarios.core.contrasena;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.generico.usuarios.core.config.UsuariosProperties;

class PoliticaContrasenasTest {

    private static final String EMAIL = "ana.garcia@ejemplo.com";
    private static final String NOMBRE = "Ana García";

    private final PoliticaContrasenas politica = new PoliticaContrasenas(
            new UsuariosProperties.Contrasena(12, 128, false, false));

    @Test
    void longitudEntre12Y128() {
        assertThat(politica.validar("a".repeat(5) + "qwerty", EMAIL, NOMBRE)).containsExactly(PoliticaContrasenas.LONGITUD_MINIMA);
        assertThat(politica.validar("caballo grapa", EMAIL, NOMBRE)).isEmpty();
        assertThat(politica.validar("x".repeat(128), EMAIL, NOMBRE)).isEmpty();
        assertThat(politica.validar("x".repeat(129), EMAIL, NOMBRE)).containsExactly(PoliticaContrasenas.LONGITUD_MAXIMA);
    }

    @Test
    void cuentaPuntosDeCodigoTrasNfkc() {
        // 11 emojis fuera del plano básico: 22 unidades UTF-16 pero 11 caracteres
        assertThat(politica.validar("😀".repeat(11), EMAIL, NOMBRE)).containsExactly(PoliticaContrasenas.LONGITUD_MINIMA);
        assertThat(politica.validar("😀".repeat(12), EMAIL, NOMBRE)).isEmpty();
        // La ligadura «ﬀ» se normaliza a «ff» (2 caracteres)
        assertThat(politica.validar("ﬀ".repeat(6), EMAIL, NOMBRE)).isEmpty();
    }

    @Test
    void aceptaEspaciosYCaracteresNoLatinos() {
        assertThat(politica.validar("тихий дом у реки", EMAIL, NOMBRE)).isEmpty();
        assertThat(politica.validar("静かな川のほとりの古い家", EMAIL, NOMBRE)).isEmpty();
    }

    @Test
    void rechazaContrasenasDeLaListaSinDistinguirMayusculas() {
        assertThat(politica.validar("unbelievable", EMAIL, NOMBRE)).containsExactly(PoliticaContrasenas.PROHIBIDA);
        assertThat(politica.validar("UnBelievable", EMAIL, NOMBRE)).containsExactly(PoliticaContrasenas.PROHIBIDA);
    }

    @Test
    void rechazaSiContieneElEmailOElNombre() {
        assertThat(politica.validar("mi clave ANA.GARCIA 2026", EMAIL, NOMBRE)).containsExactly(PoliticaContrasenas.DATOS_PERSONALES);
        assertThat(politica.validar("soy ana garcía y ya", EMAIL, NOMBRE)).containsExactly(PoliticaContrasenas.DATOS_PERSONALES);
    }

    @Test
    void sinReglasDeComposicionPorDefecto() {
        assertThat(politica.validar("todo en minusculas", EMAIL, NOMBRE)).isEmpty();
    }

    @Test
    void reglasDeComposicionActivables() {
        var estricta = new PoliticaContrasenas(new UsuariosProperties.Contrasena(12, 128, true, true));

        assertThat(estricta.validar("todoenminusculas", EMAIL, NOMBRE))
                .containsExactly(PoliticaContrasenas.MAYUSCULA, PoliticaContrasenas.CARACTER_ESPECIAL);
        assertThat(estricta.validar("Con Mayuscula y espacio", EMAIL, NOMBRE)).isEmpty();
    }
}
