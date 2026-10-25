package com.generico.usuarios.core.soporte;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;

import com.generico.usuarios.core.email.NotificadorEmailEnMemoria;

/**
 * Sustituye el email por uno en memoria, el reloj por uno controlado y el ejecutor de emails por
 * uno síncrono, para que los tests vean los emails sin esperas.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ConfiguracionTest {

    @Bean
    NotificadorEmailEnMemoria notificadorEmail() {
        return new NotificadorEmailEnMemoria();
    }

    @Bean
    @Primary
    RelojControlado relojControlado() {
        return new RelojControlado();
    }

    @Bean
    SyncTaskExecutor ejecutorEmail() {
        return new SyncTaskExecutor();
    }
}
