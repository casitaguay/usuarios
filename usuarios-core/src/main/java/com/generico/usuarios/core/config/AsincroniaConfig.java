package com.generico.usuarios.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa {@code @Async} y {@code @Scheduled}. El ejecutor {@code ejecutorEmail} está en {@link ConfiguracionPorDefecto}. */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableScheduling
public class AsincroniaConfig {
}
