package com.generico.usuarios.web.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.generico.usuarios.core.config.UsuariosProperties;

/**
 * Formulario de inicio de sesión. El envío ({@code POST /acceso}) lo procesa Spring Security; la
 * vista muestra el mensaje según el parámetro {@code error}, {@code pendiente}, {@code salida} o {@code recuperada}.
 */
@Controller
public class ControladorAcceso {

    private final UsuariosProperties propiedades;

    public ControladorAcceso(UsuariosProperties propiedades) {
        this.propiedades = propiedades;
    }

    @GetMapping("/acceso")
    String acceso(Model modelo) {
        modelo.addAttribute("registroHabilitado", propiedades.registro().habilitado());
        return "acceso";
    }
}
