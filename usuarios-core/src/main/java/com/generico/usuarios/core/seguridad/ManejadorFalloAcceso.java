package com.generico.usuarios.core.seguridad;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/** Cuenta pendiente con contraseña correcta → aviso de verificación; cualquier otro fallo → mensaje genérico (FR-028). */
public class ManejadorFalloAcceso implements AuthenticationFailureHandler {

    private final DefaultRedirectStrategy redireccion = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationFailure(HttpServletRequest peticion, HttpServletResponse respuesta,
            AuthenticationException excepcion) throws IOException {
        String destino = excepcion instanceof CuentaPendienteException ? "/acceso?pendiente" : "/acceso?error";
        redireccion.sendRedirect(peticion, respuesta, destino);
    }
}
