package com.generico.usuarios.core.config;

import java.util.EnumSet;

import jakarta.servlet.SessionTrackingMode;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Sesión HTTP (research R6). La inactividad y la cookie se fijan por código y no con
 * {@code server.servlet.session.*}, que solo aplica al Tomcat embebido (research R3).
 */
@Configuration(proxyBeanMethods = false)
public class SesionConfig {

    /** Avisa al registro de sesiones cuando una sesión se destruye o cambia de identificador. */
    @Bean
    HttpSessionEventPublisher publicadorEventosSesion() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    ServletContextInitializer configuracionSesion(UsuariosProperties propiedades) {
        return contexto -> {
            // El contenedor trabaja en minutos; 0 significaría sesión sin caducidad
            contexto.setSessionTimeout(Math.max(1, (int) propiedades.sesion().inactividad().toMinutes()));
            contexto.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE));
            var cookie = contexto.getSessionCookieConfig();
            cookie.setName("USUARIOS_SESION");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setAttribute("SameSite", "Lax");
        };
    }
}
