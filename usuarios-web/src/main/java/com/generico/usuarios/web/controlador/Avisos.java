package com.generico.usuarios.web.controlador;

import org.springframework.ui.Model;

/** Prepara la vista genérica {@code aviso}: un título, un texto y, opcionalmente, un enlace. Recibe claves de mensaje. */
final class Avisos {

    static final String VISTA = "aviso";

    private Avisos() {
    }

    static String mostrar(Model modelo, String titulo, String texto) {
        modelo.addAttribute("titulo", titulo);
        modelo.addAttribute("texto", texto);
        return VISTA;
    }

    static String mostrar(Model modelo, String titulo, String texto, String enlace, String textoEnlace) {
        modelo.addAttribute("enlace", enlace);
        modelo.addAttribute("textoEnlace", textoEnlace);
        return mostrar(modelo, titulo, texto);
    }
}
