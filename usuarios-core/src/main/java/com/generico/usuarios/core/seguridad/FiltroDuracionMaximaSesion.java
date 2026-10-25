package com.generico.usuarios.core.seguridad;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.generico.usuarios.core.config.UsuariosProperties;

/**
 * Duración de la sesión autenticada (FR-015): en la primera petición tras iniciar sesión anota la
 * hora de inicio y fija la inactividad; pasada la duración máxima invalida la sesión y la petición
 * sigue como anónima, de modo que una página privada redirige al inicio de sesión.
 */
public class FiltroDuracionMaximaSesion extends OncePerRequestFilter {

    static final String INICIO_SESION = FiltroDuracionMaximaSesion.class.getName() + ".inicio";

    private final UsuariosProperties.Sesion configuracion;
    private final Clock reloj;

    public FiltroDuracionMaximaSesion(UsuariosProperties.Sesion configuracion, Clock reloj) {
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        var sesion = peticion.getSession(false);
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (sesion != null && autenticacion != null && !(autenticacion instanceof AnonymousAuthenticationToken)) {
            Instant ahora = reloj.instant();
            if (!(sesion.getAttribute(INICIO_SESION) instanceof Instant inicio)) {
                sesion.setAttribute(INICIO_SESION, ahora);
                sesion.setMaxInactiveInterval((int) configuracion.inactividad().toSeconds());
            } else if (!ahora.isBefore(inicio.plus(configuracion.duracionMaxima()))) {
                sesion.invalidate();
                SecurityContextHolder.clearContext();
            }
        }
        cadena.doFilter(peticion, respuesta);
    }
}
