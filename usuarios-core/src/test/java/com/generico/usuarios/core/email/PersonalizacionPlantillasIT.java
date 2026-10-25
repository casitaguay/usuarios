package com.generico.usuarios.core.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;

import com.generico.usuarios.core.config.UsuariosProperties;
import com.generico.usuarios.core.soporte.BaseIT;

/**
 * Sustitución de plantillas y textos por un proyecto derivado (research R18). Solo usa nombres
 * exclusivos de test, porque estos recursos viajan en el test-jar.
 */
@TestPropertySource(properties = "spring.messages.basename=mensajes-prueba,usuarios/mensajes")
class PersonalizacionPlantillasIT extends BaseIT {

    @Autowired
    MessageSource mensajes;

    @Autowired
    UsuariosProperties propiedades;

    private final JavaMailSender enviador = mock(JavaMailSender.class);
    private NotificadorEmailSmtp notificador;

    @BeforeEach
    void crearNotificador() {
        when(enviador.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        notificador = new NotificadorEmailSmtp(enviador, mensajes, propiedades);
    }

    @Test
    void unaPlantillaPersonalizadaSustituyeALaDelCore() throws Exception {
        String contenido = enviar(new MensajeEmail("ana@ejemplo.com", "prueba", Map.of()));

        assertThat(contenido).contains("PLANTILLA-PERSONALIZADA-HTML", "PLANTILLA-PERSONALIZADA-TXT")
                .doesNotContain("PLANTILLA-CORE");
    }

    @Test
    void sinPlantillaPersonalizadaSeUsaLaDelCoreYElAsuntoDelProyectoPrevalece() throws Exception {
        var mensaje = new MensajeEmail("ana@ejemplo.com", "contrasena-cambiada",
                Map.of("nombre", "Ana", "fechaHora", "2026-01-01 10:00", "urlRecuperacion", "http://localhost/recuperacion"));

        String contenido = enviar(mensaje);

        assertThat(contenido).contains("http://localhost/recuperacion");
        assertThat(ultimo().getSubject()).isEqualTo("ASUNTO-DEL-PROYECTO");
    }

    private String enviar(MensajeEmail mensaje) throws Exception {
        notificador.enviar(mensaje);
        var salida = new ByteArrayOutputStream();
        ultimo().writeTo(salida);
        return salida.toString();
    }

    private MimeMessage ultimo() {
        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(enviador, atLeastOnce()).send(captor.capture());
        return captor.getValue();
    }
}
