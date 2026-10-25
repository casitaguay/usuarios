package com.generico.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Arranque de la aplicación, con Tomcat embebido o como WAR en Tomcat 11 (research R3). Está en
 * {@code com.generico.usuarios} para que el escaneo y las entidades JPA cubran el core y la web.
 */
@SpringBootApplication
public class UsuariosApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(UsuariosApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(UsuariosApplication.class);
    }
}
