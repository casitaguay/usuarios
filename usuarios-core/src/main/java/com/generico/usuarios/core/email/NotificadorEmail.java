package com.generico.usuarios.core.email;

/** Puerto de salida: los proyectos derivados pueden aportar otra implementación (otro proveedor, cola, etc.). */
public interface NotificadorEmail {

    void enviar(MensajeEmail mensaje);
}
