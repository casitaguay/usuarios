package com.generico.usuarios.core.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;

import com.generico.usuarios.core.email.NotificadorEmail;
import com.generico.usuarios.core.email.NotificadorEmailSmtp;

/**
 * Beans que un proyecto derivado puede sustituir declarando el suyo. Es una autoconfiguración
 * (registrada en {@code AutoConfiguration.imports}) porque {@code @ConditionalOnMissingBean} solo
 * es fiable si se evalúa después de los beans del proyecto (research R6).
 */
@AutoConfiguration
public class ConfiguracionPorDefecto {

    @Bean
    @ConditionalOnMissingBean(NotificadorEmail.class)
    NotificadorEmail notificadorEmail(JavaMailSender enviador, MessageSource mensajes, UsuariosProperties propiedades) {
        return new NotificadorEmailSmtp(enviador, mensajes, propiedades);
    }

    @Bean
    @ConditionalOnMissingBean(name = "ejecutorEmail")
    ThreadPoolTaskExecutor ejecutorEmail() {
        var ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(2);
        ejecutor.setMaxPoolSize(4);
        ejecutor.setQueueCapacity(100);
        ejecutor.setThreadNamePrefix("email-");
        return ejecutor;
    }

    @Bean
    @ConditionalOnMissingBean(SessionRegistry.class)
    SessionRegistry registroSesiones() {
        return new SessionRegistryImpl();
    }

    /** Con Spring Session en el classpath, el registro usa las sesiones compartidas (SC-010). Se procesa antes que el bean anterior. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(FindByIndexNameSessionRepository.class)
    static class SesionesCompartidas {

        @Bean
        @ConditionalOnMissingBean(SessionRegistry.class)
        <S extends Session> SessionRegistry registroSesionesCompartidas(
                ObjectProvider<FindByIndexNameSessionRepository<S>> repositorio) {
            FindByIndexNameSessionRepository<S> sesiones = repositorio.getIfAvailable();
            return sesiones != null ? new SpringSessionBackedSessionRegistry<>(sesiones) : new SessionRegistryImpl();
        }
    }
}
