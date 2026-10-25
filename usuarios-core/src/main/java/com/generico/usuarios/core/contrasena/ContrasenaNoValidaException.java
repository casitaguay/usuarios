package com.generico.usuarios.core.contrasena;

import java.util.List;

/** La contraseña incumple la política; lleva las claves de mensaje de {@link PoliticaContrasenas}. */
public class ContrasenaNoValidaException extends RuntimeException {

    private final List<String> incumplidos;

    public ContrasenaNoValidaException(List<String> incumplidos) {
        super("La contraseña incumple la política: " + incumplidos);
        this.incumplidos = List.copyOf(incumplidos);
    }

    public List<String> getIncumplidos() {
        return incumplidos;
    }
}
