package com.generico.usuarios.core.seguridad;

import org.springframework.security.authentication.AccountStatusException;

/** Contraseña correcta, pero el email aún no está verificado (FR-008). Es el único caso que se distingue (FR-028). */
public class CuentaPendienteException extends AccountStatusException {

    public CuentaPendienteException() {
        super("Cuenta pendiente de verificación");
    }
}
