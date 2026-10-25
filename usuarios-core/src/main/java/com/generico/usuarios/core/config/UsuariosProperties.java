package com.generico.usuarios.core.config;

import java.time.Duration;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades {@code usuarios.*} (contracts/configuracion.md). Los valores por defecto son los de
 * la especificación; se validan al arrancar.
 */
@Validated
@ConfigurationProperties("usuarios")
public record UsuariosProperties(
        @NotBlank String urlPublica,
        @Valid @DefaultValue Registro registro,
        @Valid @DefaultValue Contrasena contrasena,
        @Valid @DefaultValue Verificacion verificacion,
        @Valid @DefaultValue Recuperacion recuperacion,
        @Valid @DefaultValue Bloqueo bloqueo,
        @Valid @DefaultValue LimitePeticiones limitePeticiones,
        @Valid @DefaultValue Sesion sesion,
        @Valid @DefaultValue Legal legal,
        @Valid @NotNull Email email,
        @Valid @DefaultValue Limpieza limpieza) {

    public record Registro(@DefaultValue("true") boolean habilitado) {
    }

    public record Contrasena(
            @DefaultValue("12") @Min(1) int longitudMinima,
            @DefaultValue("128") @Min(1) int longitudMaxima,
            @DefaultValue("false") boolean exigirMayuscula,
            @DefaultValue("false") boolean exigirCaracterEspecial) {
    }

    public record Verificacion(
            @DefaultValue("24h") Duration validez,
            @DefaultValue("7d") Duration borrarPendientesTras) {
    }

    public record Recuperacion(@DefaultValue("1h") Duration validez) {
    }

    public record Bloqueo(
            @DefaultValue("5") @Min(1) int intentos,
            @DefaultValue("15m") Duration duracion) {
    }

    public record LimitePeticiones(
            @DefaultValue("20") @Min(1) int porMinuto,
            @DefaultValue("5m") Duration freno,
            @DefaultValue List<String> proxiesConfianza) {
    }

    public record Sesion(
            @DefaultValue("30m") Duration inactividad,
            @DefaultValue("12h") Duration duracionMaxima) {
    }

    public record Legal(@Valid @DefaultValue Documento condiciones, @Valid @DefaultValue Documento privacidad) {
    }

    public record Documento(@DefaultValue("1.0-ejemplo") @NotBlank String version) {
    }

    public record Email(@NotBlank String remitente) {
    }

    public record Limpieza(@DefaultValue("0 0 3 * * *") String cron) {
    }
}
