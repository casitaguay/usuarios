package com.generico.usuarios.web.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.generico.usuarios.core.config.UsuariosProperties;

/** Textos legales vigentes con su versión (FR-004). Cada derivado sustituye las vistas {@code legal/*}. */
@Controller
@RequestMapping("/legal")
public class ControladorLegal {

    private final UsuariosProperties propiedades;

    public ControladorLegal(UsuariosProperties propiedades) {
        this.propiedades = propiedades;
    }

    @GetMapping("/condiciones")
    String condiciones(Model modelo) {
        modelo.addAttribute("version", propiedades.legal().condiciones().version());
        return "legal/condiciones";
    }

    @GetMapping("/privacidad")
    String privacidad(Model modelo) {
        modelo.addAttribute("version", propiedades.legal().privacidad().version());
        return "legal/privacidad";
    }
}
