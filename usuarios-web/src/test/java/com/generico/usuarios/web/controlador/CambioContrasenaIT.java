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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.generico.usuarios.core.soporte.BaseIT;

class CambioContrasenaIT extends BaseIT {

    private static final String NUEVA = "otra frase bastante larga";

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuariosDePrueba usuarios;

    @BeforeEach
    void crearCuenta() {
        usuarios.activo("Ana", "ana@ejemplo.com");
    }

    @Test
    void sinSesionSeRedirigeAlAcceso() throws Exception {
        mvc.perform(get("/cuenta/contrasena")).andExpect(redirectedUrl("/acceso"));
        mvc.perform(cambio(UsuariosDePrueba.CONTRASENA, NUEVA, NUEVA)).andExpect(redirectedUrl("/acceso"));
    }

    @Test
    void conSesionSeMuestraElFormularioConLosRequisitos() throws Exception {
        mvc.perform(get("/cuenta/contrasena").session(iniciarSesion()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"contrasenaActual\"")))
                .andExpect(content().string(containsString("Entre 12 y 128 caracteres")));
    }

    @Test
    void unCambioCorrectoMantieneEstaSesionYCierraLasDemas() throws Exception {
        var actual = iniciarSesion();
        var otra = iniciarSesion();
        String idAntes = actual.getId();

        mvc.perform(cambio(UsuariosDePrueba.CONTRASENA, NUEVA, NUEVA).session(actual))
                .andExpect(redirectedUrl("/inicio?contrasenaCambiada"));

        assertThat(actual.getId()).as("nuevo identificador de sesión (FR-016)").isNotEqualTo(idAntes);
        mvc.perform(get("/inicio").session(actual)).andExpect(status().isOk());
        mvc.perform(get("/inicio").session(otra)).andExpect(redirectedUrl("/acceso"));
        mvc.perform(AccesoIT.acceso("ana@ejemplo.com", NUEVA)).andExpect(redirectedUrl("/inicio"));
    }

    @Test
    void losErroresSeMuestranEnSuCampo() throws Exception {
        var sesion = iniciarSesion();

        mvc.perform(cambio("contrasena equivocada", NUEVA, NUEVA).session(sesion))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("formulario", "contrasenaActual", "contrasena.actual-incorrecta"))
                .andExpect(content().string(not(containsString(NUEVA))));
        mvc.perform(cambio(UsuariosDePrueba.CONTRASENA, NUEVA, "otra distinta cualquiera").session(sesion))
                .andExpect(model().attributeHasFieldErrors("formulario", "confirmacion"));
        mvc.perform(cambio(UsuariosDePrueba.CONTRASENA, UsuariosDePrueba.CONTRASENA, UsuariosDePrueba.CONTRASENA).session(sesion))
                .andExpect(model().attributeHasFieldErrorCode("formulario", "contrasena", "contrasena.igual-actual"))
                .andExpect(content().string(containsString("distinta de la actual")));
    }

    @Test
    void sinTokenCsrfSeRechaza() throws Exception {
        mvc.perform(post("/cuenta/contrasena").session(iniciarSesion())
                        .param("contrasenaActual", UsuariosDePrueba.CONTRASENA)
                        .param("contrasena", NUEVA).param("confirmacion", NUEVA))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession iniciarSesion() throws Exception {
        var sesion = new MockHttpSession();
        mvc.perform(AccesoIT.acceso("ana@ejemplo.com", UsuariosDePrueba.CONTRASENA).session(sesion))
                .andExpect(redirectedUrl("/inicio"));
        return sesion;
    }

    private static MockHttpServletRequestBuilder cambio(String actual, String nueva, String confirmacion) {
        return post("/cuenta/contrasena").with(csrf())
                .param("contrasenaActual", actual).param("contrasena", nueva).param("confirmacion", confirmacion);
    }
}
