package com.generico.usuarios.web.controlador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.generico.usuarios.core.soporte.BaseIT;

class AccesoIT extends BaseIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuariosDePrueba usuarios;

    @Test
    void elFormularioEsPublico() throws Exception {
        mvc.perform(get("/acceso"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"email\"")));
    }

    @Test
    void conCredencialesCorrectasLlegaAlInicioConSuNombre() throws Exception {
        usuarios.activo("Ana Pruebas", "ana@ejemplo.com");

        var resultado = mvc.perform(acceso(" ANA@ejemplo.com", UsuariosDePrueba.CONTRASENA))
                .andExpect(redirectedUrl("/inicio"))
                .andReturn();

        mvc.perform(get("/inicio").session((MockHttpSession) resultado.getRequest().getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ana Pruebas")));
    }

    @Test
    void emailInexistenteYContrasenaErroneaDanLaMismaRespuesta() throws Exception {
        usuarios.activo("Ana", "ana@ejemplo.com");

        mvc.perform(acceso("nadie@ejemplo.com", UsuariosDePrueba.CONTRASENA)).andExpect(redirectedUrl("/acceso?error"));
        mvc.perform(acceso("ana@ejemplo.com", "contrasena equivocada")).andExpect(redirectedUrl("/acceso?error"));
        mvc.perform(get("/acceso").param("error", ""))
                .andExpect(content().string(containsString("Email o contraseña incorrectos")));
    }

    @Test
    void unaCuentaPendienteSoloSeIndicaConLaContrasenaCorrecta() throws Exception {
        usuarios.pendiente("Ana", "ana@ejemplo.com");

        mvc.perform(acceso("ana@ejemplo.com", "contrasena equivocada")).andExpect(redirectedUrl("/acceso?error"));
        mvc.perform(acceso("ana@ejemplo.com", UsuariosDePrueba.CONTRASENA)).andExpect(redirectedUrl("/acceso?pendiente"));
        mvc.perform(get("/acceso").param("pendiente", ""))
                .andExpect(content().string(containsString("href=\"/verificacion/reenvio\"")));
    }

    @Test
    void cincoFallosBloqueanLaCuentaQuinceMinutosYAvisanAlTitular() throws Exception {
        usuarios.activo("Ana", "ana@ejemplo.com");
        for (int i = 0; i < 5; i++) {
            mvc.perform(acceso("ana@ejemplo.com", "contrasena equivocada"));
        }

        mvc.perform(acceso("ana@ejemplo.com", UsuariosDePrueba.CONTRASENA)).andExpect(redirectedUrl("/acceso?error"));
        assertThat(emails.enviados("cuenta-bloqueada")).singleElement()
                .satisfies(m -> assertThat(m.destinatario()).isEqualTo("ana@ejemplo.com"));

        reloj.avanzar(Duration.ofMinutes(15).plusSeconds(1));
        mvc.perform(acceso("ana@ejemplo.com", UsuariosDePrueba.CONTRASENA)).andExpect(redirectedUrl("/inicio"));
    }

    @Test
    void unAccesoCorrectoReiniciaElContadorDeFallos() throws Exception {
        usuarios.activo("Ana", "ana@ejemplo.com");
        for (int i = 0; i < 4; i++) {
            mvc.perform(acceso("ana@ejemplo.com", "contrasena equivocada"));
        }
        mvc.perform(acceso("ana@ejemplo.com", UsuariosDePrueba.CONTRASENA)).andExpect(redirectedUrl("/inicio"));

        mvc.perform(acceso("ana@ejemplo.com", "contrasena equivocada"));
        mvc.perform(acceso("ana@ejemplo.com", UsuariosDePrueba.CONTRASENA)).andExpect(redirectedUrl("/inicio"));
        assertThat(emails.enviados("cuenta-bloqueada")).isEmpty();
    }

    @Test
    void trasAutenticarseVuelveALaPaginaQuePidio() throws Exception {
        usuarios.activo("Ana", "ana@ejemplo.com");
        var sesion = new MockHttpSession();
        mvc.perform(get("/cuenta/contrasena").session(sesion)).andExpect(redirectedUrl("/acceso"));

        mvc.perform(acceso("ana@ejemplo.com", UsuariosDePrueba.CONTRASENA).session(sesion))
                .andExpect(header().string("Location", containsString("/cuenta/contrasena")));
    }

    @Test
    void elIdentificadorDeSesionCambiaAlIniciarSesion() throws Exception {
        usuarios.activo("Ana", "ana@ejemplo.com");
        var sesion = new MockHttpSession();
        String antes = sesion.getId();

        // Como un navegador que envía su cookie de sesión: sin ella no hay sesión previa que proteger
        mvc.perform(acceso("ana@ejemplo.com", UsuariosDePrueba.CONTRASENA).session(sesion)
                        .with(peticion -> {
                            peticion.setRequestedSessionId(antes);
                            return peticion;
                        }))
                .andExpect(redirectedUrl("/inicio"));

        assertThat(sesion.getId()).isNotEqualTo(antes);
    }

    static MockHttpServletRequestBuilder acceso(String email, String contrasena) {
        return post("/acceso").with(csrf()).param("email", email).param("contrasena", contrasena);
    }
}
