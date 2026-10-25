package com.generico.usuarios.core.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj UTC inyectable: todos los tiempos salen de aquí para poder controlarlos en tests. */
@Configuration(proxyBeanMethods = false)
public class RelojConfig {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
