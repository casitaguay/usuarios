package com.generico.usuarios.web.formulario;

import jakarta.validation.constraints.NotEmpty;

/** Cambio de contraseña con sesión iniciada. La política y la coincidencia se comprueban fuera. */
public class FormularioCambioContrasena {

    @NotEmpty
    private String contrasenaActual;

    @NotEmpty
    private String contrasena;

    @NotEmpty
    private String confirmacion;

    public String getContrasenaActual() {
        return contrasenaActual;
    }

    public void setContrasenaActual(String contrasenaActual) {
        this.contrasenaActual = contrasenaActual;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getConfirmacion() {
        return confirmacion;
    }

    public void setConfirmacion(String confirmacion) {
        this.confirmacion = confirmacion;
    }
}
