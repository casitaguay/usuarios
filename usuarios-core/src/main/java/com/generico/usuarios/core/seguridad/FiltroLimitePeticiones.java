package com.generico.usuarios.core.seguridad;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Frena los envíos de acceso, registro, recuperación y reenvío desde un mismo origen (FR-014).
 * Responde 429; la página la pinta la vista de error {@code error/429}.
 */
public class FiltroLimitePeticiones extends OncePerRequestFilter {

    static final Set<String> RUTAS_LIMITADAS = Set.of("/acceso", "/registro", "/recuperacion", "/verificacion/reenvio");

    private final LimitadorPeticiones limitador;

    public FiltroLimitePeticiones(LimitadorPeticiones limitador) {
        this.limitador = limitador;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        String ruta = peticion.getRequestURI().substring(peticion.getContextPath().length());
        return !"POST".equals(peticion.getMethod()) || !RUTAS_LIMITADAS.contains(ruta);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String ruta = peticion.getRequestURI().substring(peticion.getContextPath().length());
        if (!limitador.permitir(limitador.origen(peticion), ruta)) {
            respuesta.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }
        cadena.doFilter(peticion, respuesta);
    }
}
