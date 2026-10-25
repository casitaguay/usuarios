package com.generico.usuarios.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.RequestDispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import com.generico.usuarios.core.soporte.BaseIT;

class SeguridadBaseIT extends BaseIT {

    @Autowired
    MockMvc mvc;

    @Test
    void unaRutaPrivadaSinSesionRedirigeAlAcceso() throws Exception {
        mvc.perform(get("/privada-inexistente"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/acceso"));
    }

    @Test
    void healthRespondeUpSinDetalles() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", JsonCompareMode.STRICT));
    }

    @Test
    void cabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/actuator/health").secure(true))
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'none'; form-action 'self'"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "same-origin"))
                .andExpect(header().exists("Strict-Transport-Security"));
    }

    @Test
    void hstsSoloEnHttps() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    void unPostSinTokenCsrfSeRechaza() throws Exception {
        mvc.perform(post("/acceso").param("email", "ana@ejemplo.com").param("contrasena", "x"))
                .andExpect(status().isForbidden());
    }

    @Test
    void laPaginaDeErrorEsGenericaYNoMuestraTrazas() throws Exception {
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("detalle-interno-secreto"))
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "detalle-interno-secreto"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Se ha producido un error")))
                .andExpect(content().string(not(containsString("detalle-interno-secreto"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("500"))));
    }
}
