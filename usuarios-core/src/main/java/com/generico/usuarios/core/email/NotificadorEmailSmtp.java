package com.generico.usuarios.core.email;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import jakarta.mail.MessagingException;

import org.springframework.context.MessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.generico.usuarios.core.config.UsuariosProperties;

/**
 * Envío por SMTP con plantillas Thymeleaf en HTML y texto. Una plantilla en
 * {@code personalizado/email/} sustituye a la del core con el mismo nombre (research R18).
 * Se registra en {@code ConfiguracionPorDefecto} solo si el proyecto no aporta otro {@link NotificadorEmail}.
 */
public class NotificadorEmailSmtp implements NotificadorEmail {

    private static final Locale ESPANOL = Locale.of("es");

    private final JavaMailSender enviador;
    private final MessageSource mensajes;
    private final String remitente;
    private final SpringTemplateEngine plantillas;

    public NotificadorEmailSmtp(JavaMailSender enviador, MessageSource mensajes, UsuariosProperties propiedades) {
        this.enviador = enviador;
        this.mensajes = mensajes;
        this.remitente = propiedades.email().remitente();
        this.plantillas = new SpringTemplateEngine();
        plantillas.setTemplateEngineMessageSource(mensajes);
        plantillas.addTemplateResolver(resolvedor("personalizado/email/", 1, true));
        plantillas.addTemplateResolver(resolvedor("templates/usuarios/email/", 2, false));
    }

    @Override
    public void enviar(MensajeEmail mensaje) {
        var contexto = new Context(ESPANOL, mensaje.variables());
        String html = plantillas.process(mensaje.plantilla() + ".html", contexto);
        String texto = plantillas.process(mensaje.plantilla() + ".txt", contexto);
        String asunto = mensajes.getMessage("email." + mensaje.plantilla() + ".asunto", null, ESPANOL);
        try {
            var mime = enviador.createMimeMessage();
            var ayudante = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            ayudante.setFrom(remitente);
            ayudante.setTo(mensaje.destinatario());
            ayudante.setSubject(asunto);
            ayudante.setText(texto, html);
            enviador.send(mime);
        } catch (MessagingException e) {
            throw new MailSendException("No se pudo componer el email", e);
        }
    }

    private static ClassLoaderTemplateResolver resolvedor(String prefijo, int orden, boolean comprobarExistencia) {
        var resolvedor = new ClassLoaderTemplateResolver();
        resolvedor.setPrefix(prefijo);
        resolvedor.setHtmlTemplateModePatterns(Set.of("*.html"));
        resolvedor.setTextTemplateModePatterns(Set.of("*.txt"));
        resolvedor.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolvedor.setCheckExistence(comprobarExistencia);
        resolvedor.setOrder(orden);
        return resolvedor;
    }
}
