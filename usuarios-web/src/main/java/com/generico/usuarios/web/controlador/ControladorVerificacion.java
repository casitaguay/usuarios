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

import com.generico.usuarios.core.servicio.ServicioVerificacion;
import com.generico.usuarios.web.formulario.FormularioEmail;

/** Verificación del email y reenvío del enlace (historia 1). */
@Controller
@RequestMapping("/verificacion")
public class ControladorVerificacion {

    private final ServicioVerificacion verificacion;

    public ControladorVerificacion(ServicioVerificacion verificacion) {
        this.verificacion = verificacion;
    }

    @GetMapping
    String verificar(@RequestParam(name = "t", required = false) String enlace, Model modelo) {
        return switch (verificacion.verificar(enlace)) {
            case VERIFICADA -> Avisos.mostrar(modelo, "verificacion.ok.titulo", "verificacion.ok.texto",
                    "/acceso", "verificacion.iniciar-sesion");
            case YA_VERIFICADA -> Avisos.mostrar(modelo, "verificacion.ya.titulo", "verificacion.ya.texto",
                    "/acceso", "verificacion.iniciar-sesion");
            case NO_VALIDO -> Avisos.mostrar(modelo, "verificacion.no-valido.titulo", "verificacion.no-valido.texto",
                    "/verificacion/reenvio", "verificacion.pedir-otro");
        };
    }

    @GetMapping("/reenvio")
    String formularioReenvio(@ModelAttribute("formulario") FormularioEmail formulario) {
        return "verificacion-reenvio";
    }

    @PostMapping("/reenvio")
    String reenviar(@Valid @ModelAttribute("formulario") FormularioEmail formulario, BindingResult errores) {
        if (errores.hasErrors()) {
            return "verificacion-reenvio";
        }
        verificacion.reenviar(formulario.getEmail());
        return "redirect:/verificacion/reenvio/enviado";
    }

    @GetMapping("/reenvio/enviado")
    String reenvioEnviado(Model modelo) {
        return Avisos.mostrar(modelo, "verificacion.reenvio.enviado.titulo", "verificacion.reenvio.enviado.texto");
    }
}
