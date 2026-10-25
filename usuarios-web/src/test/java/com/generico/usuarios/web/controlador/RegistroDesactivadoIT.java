package com.generico.usuarios.web.controlador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.generico.usuarios.core.soporte.BaseIT;

/** Derivados sin auto-registro (FR-006). */
@TestPropertySource(properties = "usuarios.registro.habilitado=false")
class RegistroDesactivadoIT extends BaseIT {

    @Autowired
    MockMvc mvc;

    @Test
    void lasRutasDeRegistroNoExisten() throws Exception {
        mvc.perform(get("/registro")).andExpect(status().isNotFound());
        mvc.perform(get("/registro/enviado")).andExpect(status().isNotFound());
        mvc.perform(RegistroControladorIT.registro("ana@ejemplo.com", RegistroControladorIT.CONTRASENA, true, true))
                .andExpect(status().isNotFound());

        assertThat(emails.enviados()).isEmpty();
    }
}
