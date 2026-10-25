package com.generico.usuarios.web.controlador;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.generico.usuarios.core.config.UsuariosProperties;
import com.generico.usuarios.core.contrasena.ContrasenaNoValidaException;
import com.generico.usuarios.core.contrasena.PoliticaContrasenas;
import com.generico.usuarios.core.servicio.ServicioRegistro;
import com.generico.usuarios.web.formulario.FormularioRegistro;

/** Registro (historia 1). Responde igual exista o no el email (FR-028). */
@Controller
@RequestMapping("/registro")
public class ControladorRegistro {

    private final ServicioRegistro registro;
    private final PoliticaContrasenas politica;
    private final UsuariosProperties propiedades;

    public ControladorRegistro(ServicioRegistro registro, PoliticaContrasenas politica, UsuariosProperties propiedades) {
        this.registro = registro;
        this.politica = politica;
        this.propiedades = propiedades;
    }

    /** Se ejecuta antes de cada ruta: sin auto-registro, las rutas no existen (FR-006). */
    @ModelAttribute
    void comprobarRegistroHabilitado(Model modelo) {
        if (!propiedades.registro().habilitado()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        modelo.addAttribute("reglas", politica.reglas());
    }

    @GetMapping
    String formulario(@ModelAttribute("formulario") FormularioRegistro formulario) {
        return "registro";
    }

    @PostMapping
    String registrar(@Valid @ModelAttribute("formulario") FormularioRegistro formulario, BindingResult errores) {
        if (!errores.hasErrors()) {
            try {
                registro.registrar(formulario.getNombre(), formulario.getEmail(), formulario.getContrasena());
                return "redirect:/registro/enviado";
            } catch (ContrasenaNoValidaException e) {
                Object[] limites = {politica.reglas().longitudMinima(), politica.reglas().longitudMaxima()};
                e.getIncumplidos().forEach(clave -> errores.rejectValue("contrasena", clave, limites, null));
            }
        }
        formulario.setContrasena(null);
        return "registro";
    }

    @GetMapping("/enviado")
    String enviado(Model modelo) {
        return Avisos.mostrar(modelo, "registro.enviado.titulo", "registro.enviado.texto");
    }
}
