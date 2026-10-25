package com.generico.usuarios.web.controlador;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.generico.usuarios.core.config.UsuariosProperties;
import com.generico.usuarios.core.contrasena.ContrasenaNoValidaException;
import com.generico.usuarios.core.contrasena.PoliticaContrasenas;
import com.generico.usuarios.core.seguridad.UsuarioAutenticado;
import com.generico.usuarios.core.servicio.ContrasenaActualIncorrectaException;
import com.generico.usuarios.core.servicio.ServicioCambioContrasena;
import com.generico.usuarios.web.formulario.FormularioCambioContrasena;

/** Cambio de contraseña con sesión iniciada (historia 4). */
@Controller
@RequestMapping("/cuenta/contrasena")
public class ControladorCuenta {

    private final ServicioCambioContrasena cambio;
    private final PoliticaContrasenas politica;

    public ControladorCuenta(ServicioCambioContrasena cambio, PoliticaContrasenas politica) {
        this.cambio = cambio;
        this.politica = politica;
    }

    @ModelAttribute("reglas")
    UsuariosProperties.Contrasena reglas() {
        return politica.reglas();
    }

    @GetMapping
    String formulario(@ModelAttribute("formulario") FormularioCambioContrasena formulario) {
        return "cuenta-contrasena";
    }

    @PostMapping
    String cambiar(@Valid @ModelAttribute("formulario") FormularioCambioContrasena formulario, BindingResult errores,
            @AuthenticationPrincipal UsuarioAutenticado usuario, HttpServletRequest peticion) {
        ErroresContrasena.comprobarConfirmacion(errores, formulario.getContrasena(), formulario.getConfirmacion());
        if (!errores.hasErrors()) {
            try {
                cambio.cambiar(usuario.getId(), formulario.getContrasenaActual(), formulario.getContrasena(),
                        peticion.getSession().getId());
                peticion.changeSessionId(); // FR-016
                return "redirect:/inicio?contrasenaCambiada";
            } catch (ContrasenaActualIncorrectaException e) {
                errores.rejectValue("contrasenaActual", "contrasena.actual-incorrecta");
            } catch (ContrasenaNoValidaException e) {
                ErroresContrasena.rechazar(errores, e, politica.reglas());
            }
        }
        formulario.setContrasenaActual(null);
        formulario.setContrasena(null);
        formulario.setConfirmacion(null);
        return "cuenta-contrasena";
    }
}
