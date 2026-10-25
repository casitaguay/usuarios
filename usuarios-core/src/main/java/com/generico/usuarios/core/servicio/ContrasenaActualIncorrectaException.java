package com.generico.usuarios.core.servicio;

/** La contraseña actual indicada al cambiarla no es correcta; cuenta como intento fallido. */
public class ContrasenaActualIncorrectaException extends RuntimeException {

    public ContrasenaActualIncorrectaException() {
        super("La contraseña actual no es correcta");
    }
}
