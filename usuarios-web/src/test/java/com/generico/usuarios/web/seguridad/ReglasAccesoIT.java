package com.generico.usuarios.web.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;
import com.generico.usuarios.core.soporte.BaseIT;

/** Principio V: cada ruta de contracts/rutas-web.md con su caso permitido y su caso denegado. */
class ReglasAccesoIT extends BaseIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepositorio usuarios;

    @Autowired
    PasswordEncoder codificador;

    @ParameterizedTest
    @ValueSource(strings = {
        "/acceso", "/registro", "/registro/enviado",
        "/verificacion", "/verificacion/reenvio", "/verificacion/reenvio/enviado",
        "/recuperacion", "/recuperacion/enviado", "/recuperacion/nueva",
        "/legal/condiciones", "/legal/privacidad",
        "/actuator/health", "/webjars/bootstrap/css/bootstrap.min.css", "/css/aplicacion.css"
    })
    void lasRutasPublicasRespondenSinSesion(String ruta) throws Exception {
        mvc.perform(get(ruta)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/error", "/favicon.ico", "/img/no-existe.png"})
    void lasRutasPublicasSinContenidoNoPidenSesion(String ruta) throws Exception {
        var resultado = mvc.perform(get(ruta)).andReturn();
        assertThat(resultado.getResponse().getRedirectedUrl()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/inicio", "/cuenta/contrasena", "/cualquier/otra/ruta"})
    void lasRutasPrivadasPidenSesion(String ruta) throws Exception {
        mvc.perform(get(ruta)).andExpect(redirectedUrl("/acceso"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/inicio", "/cuenta/contrasena"})
    void lasRutasPrivadasRespondenConSesion(String ruta) throws Exception {
        mvc.perform(get(ruta).session(iniciarSesion())).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cuenta/contrasena"})
    void losEnviosPrivadosPidenSesion(String ruta) throws Exception {
        mvc.perform(post(ruta).with(csrf())).andExpect(redirectedUrl("/acceso"));
    }

    private MockHttpSession iniciarSesion() throws Exception {
        var usuario = new Usuario("Ana", "ana@ejemplo.com", codificador.encode("caballo grapa bateria"), reloj.instant());
        usuario.verificar(reloj.instant());
        usuarios.save(usuario);
        var sesion = new MockHttpSession();
        mvc.perform(post("/acceso").with(csrf()).session(sesion)
                        .param("email", "ana@ejemplo.com").param("contrasena", "caballo grapa bateria"))
                .andExpect(redirectedUrl("/inicio"));
        return sesion;
    }
}
