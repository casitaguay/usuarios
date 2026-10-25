package com.generico.usuarios.core.email;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Implementación de tests del puerto: guarda los mensajes en lugar de enviarlos. */
public class NotificadorEmailEnMemoria implements NotificadorEmail {

    private final List<MensajeEmail> enviados = new CopyOnWriteArrayList<>();

    @Override
    public void enviar(MensajeEmail mensaje) {
        enviados.add(mensaje);
    }

    public List<MensajeEmail> enviados() {
        return List.copyOf(enviados);
    }

    public List<MensajeEmail> enviados(String plantilla) {
        return enviados.stream().filter(m -> m.plantilla().equals(plantilla)).toList();
    }

    public void limpiar() {
        enviados.clear();
    }

    /** Valor del parámetro {@code t} del enlace que lleva el mensaje en la variable {@code enlace}. */
    public static String enlace(MensajeEmail mensaje) {
        String url = (String) mensaje.variables().get("enlace");
        return url.substring(url.indexOf("t=") + 2);
    }
}
