package com.generico.usuarios.web.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.RequestDispatcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.generico.usuarios.core.soporte.BaseIT;

/** Limitación por origen (FR-014): 20 peticiones por minuto y conexión en cada grupo de rutas. */
class LimitePeticionesIT extends BaseIT {

    @Autowired
    MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"/acceso", "/registro", "/recuperacion", "/verificacion/reenvio"})
    void laPeticion21EnUnMinutoSeFrena(String ruta) throws Exception {
        for (int i = 0; i < 20; i++) {
            mvc.perform(envio(ruta, "198.51.100.1")).andExpect(noFrenada());
        }

        mvc.perform(envio(ruta, "198.51.100.1")).andExpect(status().isTooManyRequests());
        mvc.perform(envio(ruta, "198.51.100.2")).andExpect(noFrenada());
    }

    @Test
    void losGetNoSeLimitan() throws Exception {
        for (int i = 0; i < 25; i++) {
            mvc.perform(get("/acceso")).andExpect(status().isOk());
        }
    }

    @Test
    void laPaginaDelFrenoInvitaAEsperarSinRevelarNada() throws Exception {
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 429))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string(containsString("Demasiados intentos")))
                .andExpect(content().string(not(containsString("@"))));
    }

    /** /recuperacion aún no existe (historia 3): basta con que la petición no se frene. */
    private static ResultMatcher noFrenada() {
        return resultado -> assertThat(resultado.getResponse().getStatus()).isNotEqualTo(429);
    }

    private static MockHttpServletRequestBuilder envio(String ruta, String ip) {
        return post(ruta).with(csrf()).param("email", "nadie@ejemplo.com").param("contrasena", "x")
                .with(peticion -> {
                    peticion.setRemoteAddr(ip);
                    return peticion;
                });
    }
}
