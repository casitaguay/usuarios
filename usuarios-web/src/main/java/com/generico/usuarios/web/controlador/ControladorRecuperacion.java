package com.generico.usuarios.web.controlador;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.generico.usuarios.core.contrasena.ContrasenaNoValidaException;
import com.generico.usuarios.core.contrasena.PoliticaContrasenas;
import com.generico.usuarios.core.servicio.ServicioRecuperacion;
import com.generico.usuarios.web.formulario.FormularioEmail;
import com.generico.usuarios.web.formulario.FormularioNuevaContrasena;

/** Recuperación de contraseña (historia 3). El enlace viaja en un campo oculto, nunca en una redirección. */
@Controller
@RequestMapping("/recuperacion")
public class ControladorRecuperacion {

    private final ServicioRecuperacion recuperacion;
    private final PoliticaContrasenas politica;

    public ControladorRecuperacion(ServicioRecuperacion recuperacion, PoliticaContrasenas politica) {
        this.recuperacion = recuperacion;
        this.politica = politica;
    }

    @GetMapping
    String formularioSolicitud(@ModelAttribute("formulario") FormularioEmail formulario) {
        return "recuperacion";
    }

    @PostMapping
    String solicitar(@Valid @ModelAttribute("formulario") FormularioEmail formulario, BindingResult errores) {
        if (errores.hasErrors()) {
            return "recuperacion";
        }
        recuperacion.solicitar(formulario.getEmail());
        return "redirect:/recuperacion/enviado";
    }

    @GetMapping("/enviado")
    String enviado(Model modelo) {
        return Avisos.mostrar(modelo, "recuperacion.enviado.titulo", "recuperacion.enviado.texto");
    }

    @GetMapping("/nueva")
    String formularioNueva(@RequestParam(name = "t", required = false) String enlace, Model modelo) {
        if (!recuperacion.comprobar(enlace)) {
            return enlaceNoValido(modelo);
        }
        var formulario = new FormularioNuevaContrasena();
        formulario.setT(enlace);
        modelo.addAttribute("formulario", formulario);
        modelo.addAttribute("reglas", politica.reglas());
        return "recuperacion-nueva";
    }

    @PostMapping("/nueva")
    String completar(@Valid @ModelAttribute("formulario") FormularioNuevaContrasena formulario, BindingResult errores,
            Model modelo) {
        ErroresContrasena.comprobarConfirmacion(errores, formulario.getContrasena(), formulario.getConfirmacion());
        if (!errores.hasErrors()) {
            try {
                if (!recuperacion.completar(formulario.getT(), formulario.getContrasena())) {
                    return enlaceNoValido(modelo);
                }
                return "redirect:/acceso?recuperada";
            } catch (ContrasenaNoValidaException e) {
                ErroresContrasena.rechazar(errores, e, politica.reglas());
            }
        }
        formulario.setContrasena(null);
        formulario.setConfirmacion(null);
        modelo.addAttribute("reglas", politica.reglas());
        return "recuperacion-nueva";
    }

    private static String enlaceNoValido(Model modelo) {
        return Avisos.mostrar(modelo, "recuperacion.no-valido.titulo", "recuperacion.no-valido.texto",
                "/recuperacion", "recuperacion.pedir-otro");
    }
}
