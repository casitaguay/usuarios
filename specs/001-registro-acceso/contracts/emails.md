# Contrato: emails enviados

**Feature**: `001-registro-acceso`

Todos los emails se envían de forma asíncrona tras confirmar la transacción (research R8), en
español, en versión HTML y texto plano. Las plantillas son sobreescribibles por los proyectos
derivados (`templates/email/*`). Ningún email incluye la contraseña. Los enlaces usan la URL
pública configurada en `usuarios.url-publica`.

| Plantilla | Cuándo se envía | Contenido mínimo | Requisito |
|---|---|---|---|
| `verificacion` | Registro nuevo y reenvío | Saludo con el nombre, enlace de verificación, validez de 24 h, aviso de ignorarlo si no se registró | FR-007, FR-009 |
| `registro-existente` | Intento de registro con un email que ya tiene cuenta | Aviso del intento, enlace a la recuperación de contraseña, indicación de ignorarlo si fue él | Historia 1, escenario 5 |
| `recuperacion` | Solicitud de recuperación con email existente | Enlace de recuperación, validez de 1 h, aviso de ignorarlo si no lo pidió | FR-019 |
| `contrasena-cambiada` | Recuperación completada o cambio de contraseña | Confirmación con fecha y hora, indicación de contactar o recuperar la cuenta si no fue él | FR-021, FR-022 |
| `cuenta-bloqueada` | La cuenta alcanza 5 intentos fallidos | Aviso del bloqueo de 15 min y enlace a la recuperación de contraseña | FR-012 |

Puerto en `usuarios-core`:

```java
// Puerto de salida: los proyectos derivados pueden aportar otra implementación (otro proveedor, cola, etc.)
public interface NotificadorEmail {
    void enviar(MensajeEmail mensaje);
}
```

`MensajeEmail` contiene destinatario, plantilla y variables. La implementación por defecto usa SMTP;
la de tests guarda los mensajes en memoria.
