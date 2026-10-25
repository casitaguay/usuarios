package com.generico.usuarios.web.controlador;

import java.util.Objects;

import org.springframework.validation.BindingResult;

import com.generico.usuarios.core.config.UsuariosProperties;
import com.generico.usuarios.core.contrasena.ContrasenaNoValidaException;

/** Lleva los errores de contraseña a los campos {@code contrasena} y {@code confirmacion} del formulario. */
final class ErroresContrasena {

    private ErroresContrasena() {
    }

    /** Los mensajes de la política reciben como argumentos la longitud mínima y la máxima. */
    static void rechazar(BindingResult errores, ContrasenaNoValidaException excepcion, UsuariosProperties.Contrasena reglas) {
        Object[] limites = {reglas.longitudMinima(), reglas.longitudMaxima()};
        excepcion.getIncumplidos().forEach(clave -> errores.rejectValue("contrasena", clave, limites, null));
    }

    static void comprobarConfirmacion(BindingResult errores, String contrasena, String confirmacion) {
        if (!Objects.equals(contrasena, confirmacion)) {
            errores.rejectValue("confirmacion", "contrasena.confirmacion");
        }
    }
}
