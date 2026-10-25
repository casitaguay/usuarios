package com.generico.usuarios.core.config;

import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/** Seguridad web (principio I, research R5, R6 y R11). Todo es privado salvo las rutas públicas declaradas. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UsuariosProperties.class)
public class SeguridadConfig {

    /** Rutas públicas de contracts/rutas-web.md. */
    static final String[] RUTAS_PUBLICAS = {
        "/acceso",
        "/registro", "/registro/enviado",
        "/verificacion", "/verificacion/reenvio", "/verificacion/reenvio/enviado",
        "/recuperacion", "/recuperacion/enviado", "/recuperacion/nueva",
        "/legal/condiciones", "/legal/privacidad",
        "/error", "/actuator/health",
        "/webjars/**", "/css/**", "/img/**", "/favicon.ico"
    };

    private static final String CSP = "default-src 'self'; frame-ancestors 'none'; form-action 'self'";

    /** Argon2id por defecto; el prefijo {argon2} permite migrar de algoritmo sin invalidar hashes (research R4). */
    @Bean
    PasswordEncoder passwordEncoder() {
        // Parámetros mínimos de OWASP para Argon2id: 19 MiB, 2 iteraciones, paralelismo 1
        var argon2 = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
        return new DelegatingPasswordEncoder("argon2", Map.of("argon2", argon2));
    }

    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http, SessionRegistry registroSesiones) throws Exception {
        http
            .authorizeHttpRequests(peticiones -> peticiones
                .requestMatchers(RUTAS_PUBLICAS).permitAll()
                .anyRequest().authenticated())
            .formLogin(formulario -> formulario
                .loginPage("/acceso")
                .usernameParameter("email")
                .passwordParameter("contrasena")
                .defaultSuccessUrl("/inicio")
                .failureUrl("/acceso?error"))
            .logout(salida -> salida
                .logoutUrl("/salir")
                .logoutSuccessUrl("/acceso?salida"))
            .headers(cabeceras -> cabeceras
                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN)))
            .sessionManagement(sesion -> sesion
                .sessionFixation(fijacion -> fijacion.changeSessionId())
                // -1: sin límite de sesiones simultáneas (FR-037); el registro permite cerrarlas todas
                .sessionConcurrency(concurrencia -> concurrencia
                    .maximumSessions(-1)
                    .sessionRegistry(registroSesiones)));
        return http.build();
    }
}
