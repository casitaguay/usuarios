package com.generico.usuarios.web.controlador;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.generico.usuarios.core.seguridad.UsuarioAutenticado;

/** Página de inicio de la persona autenticada (FR-032). */
@Controller
public class ControladorInicio {

    @GetMapping("/inicio")
    String inicio(@AuthenticationPrincipal UsuarioAutenticado usuario, Model modelo) {
        modelo.addAttribute("nombre", usuario.getNombre());
        return "inicio";
    }
}
