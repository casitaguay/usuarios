package com.generico.usuarios.web.formulario;

import jakarta.validation.constraints.NotEmpty;

/**
 * Nueva contraseña con el enlace de recuperación ({@code t}, en un campo oculto). La coincidencia
 * con la confirmación la comprueba el controlador para mostrar el error en ese campo.
 */
public class FormularioNuevaContrasena {

    // Sin validación: un enlace vacío o inventado lo trata el servicio como no válido
    private String t;

    @NotEmpty
    private String contrasena;

    @NotEmpty
    private String confirmacion;

    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
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
