package com.generico.usuarios.web.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.generico.usuarios.core.email.NotificadorEmailEnMemoria;
import com.generico.usuarios.core.soporte.BaseIT;

/** FR-027 y FR-031: los logs no contienen contraseñas, enlaces, emails ni nombres. */
@ExtendWith(OutputCaptureExtension.class)
class LogsSinDatosSensiblesIT extends BaseIT {

    private static final String NOMBRE = "Zacarías Inventadez";
    private static final String EMAIL = "zeta.privada@ejemplo.com";
    private static final String CONTRASENA = "caballo grapa bateria";
    private static final String NUEVA = "otra frase bastante larga";
    private static final String FINAL = "una tercera frase larga";

    @Autowired
    MockMvc mvc;

    @Test
    void elRecorridoCompletoNoDejaDatosSensiblesEnLosLogs(CapturedOutput salida) throws Exception {
        mvc.perform(post("/registro").with(csrf()).param("nombre", NOMBRE).param("email", EMAIL)
                .param("contrasena", CONTRASENA).param("aceptaCondiciones", "true").param("aceptaPrivacidad", "true"));
        String verificacion = enlace("verificacion");
        mvc.perform(get("/verificacion").param("t", verificacion));

        mvc.perform(post("/acceso").with(csrf()).param("email", EMAIL).param("contrasena", "contrasena equivocada"));

        mvc.perform(post("/recuperacion").with(csrf()).param("email", EMAIL));
        String recuperacion = enlace("recuperacion");
        mvc.perform(post("/recuperacion/nueva").with(csrf())
                .param("t", recuperacion).param("contrasena", NUEVA).param("confirmacion", NUEVA));

        var sesion = new MockHttpSession();
        mvc.perform(post("/acceso").with(csrf()).session(sesion).param("email", EMAIL).param("contrasena", NUEVA));
        mvc.perform(post("/cuenta/contrasena").with(csrf()).session(sesion)
                .param("contrasenaActual", NUEVA).param("contrasena", FINAL).param("confirmacion", FINAL));

        assertThat(emails.enviados("contrasena-cambiada")).as("el recorrido llegó al final").hasSize(2);
        assertThat(salida.getAll()).doesNotContain(
                CONTRASENA, NUEVA, FINAL, verificacion, recuperacion, EMAIL, "zeta.privada", NOMBRE, "Inventadez");
    }

    private String enlace(String plantilla) {
        return NotificadorEmailEnMemoria.enlace(emails.enviados(plantilla).getLast());
    }
}
