package com.generico.usuarios.web.controlador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.generico.usuarios.core.soporte.BaseIT;

class RegistroControladorIT extends BaseIT {

    static final String CONTRASENA = "caballo grapa bateria";

    @Autowired
    MockMvc mvc;

    @Test
    void elFormularioEsPublico() throws Exception {
        mvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"aceptaCondiciones\"")))
                .andExpect(content().string(not(containsString("checked"))));
    }

    @Test
    void unRegistroValidoYUnEmailExistenteRedirigenIgual() throws Exception {
        mvc.perform(registro("ana@ejemplo.com", CONTRASENA, true, true))
                .andExpect(redirectedUrl("/registro/enviado"));
        mvc.perform(registro("ana@ejemplo.com", CONTRASENA, true, true))
                .andExpect(redirectedUrl("/registro/enviado"));

        assertThat(emails.enviados()).extracting(m -> m.plantilla())
                .containsExactly("verificacion", "registro-existente");
        mvc.perform(get("/registro/enviado"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("recibirás un email")));
    }

    @Test
    void sinAceptarLasCasillasNoSeRegistraYLaContrasenaNoSeDevuelve() throws Exception {
        mvc.perform(registro("ana@ejemplo.com", CONTRASENA, false, false))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("formulario", "aceptaCondiciones", "aceptaPrivacidad"))
                .andExpect(content().string(not(containsString(CONTRASENA))));

        assertThat(emails.enviados()).isEmpty();
    }

    @Test
    void unaContrasenaQueIncumpleLaPoliticaSeIndicaEnSuCampo() throws Exception {
        mvc.perform(registro("ana@ejemplo.com", "corta-1234", true, true))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("formulario", "contrasena", "contrasena.longitud-minima"))
                .andExpect(content().string(containsString("al menos 12 caracteres")))
                .andExpect(content().string(not(containsString("corta-1234"))));
    }

    @Test
    void sinTokenCsrfSeRechaza() throws Exception {
        mvc.perform(post("/registro").param("nombre", "Ana").param("email", "ana@ejemplo.com")
                        .param("contrasena", CONTRASENA).param("aceptaCondiciones", "true").param("aceptaPrivacidad", "true"))
                .andExpect(status().isForbidden());
    }

    static MockHttpServletRequestBuilder registro(String email, String contrasena, boolean condiciones, boolean privacidad) {
        var peticion = post("/registro").with(csrf())
                .param("nombre", "Ana").param("email", email).param("contrasena", contrasena);
        if (condiciones) {
            peticion.param("aceptaCondiciones", "true");
        }
        if (privacidad) {
            peticion.param("aceptaPrivacidad", "true");
        }
        return peticion;
    }
}
