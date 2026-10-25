package com.generico.usuarios.web.controlador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.generico.usuarios.core.soporte.BaseIT;

class SesionIT extends BaseIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuariosDePrueba usuarios;

    @BeforeEach
    void crearCuenta() {
        usuarios.activo("Ana", "ana@ejemplo.com");
    }

    @Test
    void elInicioRequiereSesion() throws Exception {
        mvc.perform(get("/inicio")).andExpect(redirectedUrl("/acceso"));
        mvc.perform(get("/inicio").session(iniciarSesion())).andExpect(status().isOk());
    }

    @Test
    void cerrarSesionPorPostInvalidaLaSesion() throws Exception {
        var sesion = iniciarSesion();

        mvc.perform(post("/salir").with(csrf()).session(sesion)).andExpect(redirectedUrl("/acceso?salida"));

        assertThat(sesion.isInvalid()).isTrue();
        mvc.perform(get("/inicio").session(sesion)).andExpect(redirectedUrl("/acceso"));
    }

    @Test
    void unGetASalirNoCierraLaSesion() throws Exception {
        var sesion = iniciarSesion();

        mvc.perform(get("/salir").session(sesion));

        mvc.perform(get("/inicio").session(sesion)).andExpect(status().isOk());
    }

    @Test
    void laSesionCaducaALasDoceHorasAunqueSeUse() throws Exception {
        var sesion = iniciarSesion();
        mvc.perform(get("/inicio").session(sesion)).andExpect(status().isOk());

        reloj.avanzar(Duration.ofHours(12).minusSeconds(1));
        mvc.perform(get("/inicio").session(sesion)).andExpect(status().isOk());

        reloj.avanzar(Duration.ofSeconds(2));
        mvc.perform(get("/inicio").session(sesion)).andExpect(redirectedUrl("/acceso"));
    }

    @Test
    void seAplicaLaInactividadConfigurada() throws Exception {
        var sesion = iniciarSesion();
        mvc.perform(get("/inicio").session(sesion));

        assertThat(sesion.getMaxInactiveInterval()).isEqualTo(30 * 60);
    }

    @Test
    void dosSesionesDelMismoUsuarioSonValidasALaVez() throws Exception {
        var primera = iniciarSesion();
        var segunda = iniciarSesion();

        mvc.perform(get("/inicio").session(primera)).andExpect(status().isOk());
        mvc.perform(get("/inicio").session(segunda)).andExpect(status().isOk());
    }

    private MockHttpSession iniciarSesion() throws Exception {
        var sesion = new MockHttpSession();
        mvc.perform(AccesoIT.acceso("ana@ejemplo.com", UsuariosDePrueba.CONTRASENA).session(sesion))
                .andExpect(redirectedUrl("/inicio"));
        return sesion;
    }
}
