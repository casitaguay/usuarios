package com.generico.usuarios.web.controlador;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.generico.usuarios.core.email.NotificadorEmailEnMemoria;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;
import com.generico.usuarios.core.servicio.ServicioRegistro;
import com.generico.usuarios.core.soporte.BaseIT;

class VerificacionControladorIT extends BaseIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    ServicioRegistro registro;

    @Autowired
    UsuarioRepositorio usuarios;

    @Test
    void unEnlaceValidoVerificaLaCuenta() throws Exception {
        mvc.perform(get("/verificacion").param("t", registrar()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Cuenta verificada</title>")))
                .andExpect(content().string(containsString("<h1>Cuenta verificada</h1>")))
                .andExpect(content().string(containsString("Tu cuenta está verificada")))
                .andExpect(content().string(containsString("href=\"/acceso\"")));
    }

    @Test
    void unEnlaceDeUnaCuentaYaVerificadaLoIndica() throws Exception {
        String enlace = registrar();
        var usuario = usuarios.findByEmail("ana@ejemplo.com").orElseThrow();
        usuario.verificar(reloj.instant());
        usuarios.save(usuario);

        mvc.perform(get("/verificacion").param("t", enlace))
                .andExpect(content().string(containsString("ya estaba verificada")));
    }

    @Test
    void unEnlaceNoValidoOfreceElReenvio() throws Exception {
        mvc.perform(get("/verificacion").param("t", "inventado"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no es válido")))
                .andExpect(content().string(containsString("href=\"/verificacion/reenvio\"")));
        mvc.perform(get("/verificacion"))
                .andExpect(content().string(containsString("no es válido")));
    }

    @Test
    void elReenvioEsPublicoYRespondeIgualExistaONoLaCuenta() throws Exception {
        registrar();

        mvc.perform(get("/verificacion/reenvio")).andExpect(status().isOk());
        mvc.perform(post("/verificacion/reenvio").with(csrf()).param("email", "ana@ejemplo.com"))
                .andExpect(redirectedUrl("/verificacion/reenvio/enviado"));
        mvc.perform(post("/verificacion/reenvio").with(csrf()).param("email", "nadie@ejemplo.com"))
                .andExpect(redirectedUrl("/verificacion/reenvio/enviado"));
        mvc.perform(get("/verificacion/reenvio/enviado")).andExpect(status().isOk());
    }

    @Test
    void losTextosLegalesSonPublicos() throws Exception {
        mvc.perform(get("/legal/condiciones"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TEXTO DE EJEMPLO")))
                .andExpect(content().string(containsString("1.0-ejemplo")));
        mvc.perform(get("/legal/privacidad"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TEXTO DE EJEMPLO")));
    }

    private String registrar() {
        registro.registrar("Ana", "ana@ejemplo.com", "caballo grapa bateria");
        return NotificadorEmailEnMemoria.enlace(emails.enviados("verificacion").getLast());
    }
}
