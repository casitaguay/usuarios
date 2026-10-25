package com.generico.usuarios.web.controlador;

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

import com.generico.usuarios.core.email.NotificadorEmailEnMemoria;
import com.generico.usuarios.core.soporte.BaseIT;

class RecuperacionControladorIT extends BaseIT {

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
    void laSolicitudEsPublicaYRespondeIgualExistaONoLaCuenta() throws Exception {
        mvc.perform(get("/recuperacion")).andExpect(status().isOk());
        mvc.perform(solicitud("ana@ejemplo.com")).andExpect(redirectedUrl("/recuperacion/enviado"));
        mvc.perform(solicitud("nadie@ejemplo.com")).andExpect(redirectedUrl("/recuperacion/enviado"));
        mvc.perform(get("/recuperacion/enviado"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("existe una cuenta con ese email")));
    }

    @Test
    void unEnlaceNoValidoOfrecePedirOtro() throws Exception {
        mvc.perform(get("/recuperacion/nueva").param("t", "inventado"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no es válido")))
                .andExpect(content().string(containsString("href=\"/recuperacion\"")));
        mvc.perform(nuevaContrasena("inventado", NUEVA, NUEVA))
                .andExpect(content().string(containsString("no es válido")));
    }

    @Test
    void unEnlaceValidoMuestraElFormularioConElEnlaceOculto() throws Exception {
        String enlace = solicitar();

        mvc.perform(get("/recuperacion/nueva").param("t", enlace))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("type=\"hidden\"")))
                .andExpect(content().string(containsString("name=\"confirmacion\"")));
    }

    @Test
    void completarRedirigeAlAccesoYCierraLasSesionesAbiertas() throws Exception {
        var sesion = new MockHttpSession();
        mvc.perform(AccesoIT.acceso("ana@ejemplo.com", UsuariosDePrueba.CONTRASENA).session(sesion))
                .andExpect(redirectedUrl("/inicio"));

        mvc.perform(nuevaContrasena(solicitar(), NUEVA, NUEVA)).andExpect(redirectedUrl("/acceso?recuperada"));

        mvc.perform(get("/inicio").session(sesion)).andExpect(redirectedUrl("/acceso"));
        mvc.perform(AccesoIT.acceso("ana@ejemplo.com", NUEVA)).andExpect(redirectedUrl("/inicio"));
        mvc.perform(AccesoIT.acceso("ana@ejemplo.com", UsuariosDePrueba.CONTRASENA)).andExpect(redirectedUrl("/acceso?error"));
    }

    @Test
    void confirmacionDistintaOContrasenaInvalidaDevuelvenElFormularioConErrores() throws Exception {
        String enlace = solicitar();

        mvc.perform(nuevaContrasena(enlace, NUEVA, "otra distinta cualquiera"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("formulario", "confirmacion"))
                .andExpect(content().string(not(containsString(NUEVA))));
        mvc.perform(nuevaContrasena(enlace, "corta-123", "corta-123"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("formulario", "contrasena", "contrasena.longitud-minima"));

        // Los errores no consumen el enlace
        mvc.perform(nuevaContrasena(enlace, NUEVA, NUEVA)).andExpect(redirectedUrl("/acceso?recuperada"));
    }

    @Test
    void sinTokenCsrfSeRechaza() throws Exception {
        mvc.perform(post("/recuperacion").param("email", "ana@ejemplo.com")).andExpect(status().isForbidden());
        mvc.perform(post("/recuperacion/nueva").param("t", "x").param("contrasena", NUEVA).param("confirmacion", NUEVA))
                .andExpect(status().isForbidden());
    }

    private String solicitar() throws Exception {
        mvc.perform(solicitud("ana@ejemplo.com"));
        return NotificadorEmailEnMemoria.enlace(emails.enviados("recuperacion").getLast());
    }

    private static MockHttpServletRequestBuilder solicitud(String email) {
        return post("/recuperacion").with(csrf()).param("email", email);
    }

    private static MockHttpServletRequestBuilder nuevaContrasena(String enlace, String contrasena, String confirmacion) {
        return post("/recuperacion/nueva").with(csrf())
                .param("t", enlace).param("contrasena", contrasena).param("confirmacion", confirmacion);
    }
}
