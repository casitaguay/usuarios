package com.generico.usuarios.core.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Persona con cuenta (data-model.md). No es {@code final}: los proyectos derivados guardan sus
 * datos propios en tablas relacionadas por {@code usuario_id}.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private UUID id;

    private String nombre;

    private String email;

    @Column(name = "hash_contrasena")
    private String hashContrasena;

    @Enumerated(EnumType.STRING)
    private EstadoUsuario estado;

    @Column(name = "creado_en")
    private Instant creadoEn;

    @Column(name = "verificado_en")
    private Instant verificadoEn;

    @Column(name = "intentos_fallidos")
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "contrasena_cambiada_en")
    private Instant contrasenaCambiadaEn;

    @Version
    private Integer version;

    protected Usuario() {
    }

    public Usuario(String nombre, String email, String hashContrasena, Instant ahora) {
        this.id = UUID.randomUUID();
        this.nombre = nombre.strip();
        this.email = normalizarEmail(email);
        this.hashContrasena = hashContrasena;
        this.estado = EstadoUsuario.PENDIENTE_VERIFICACION;
        this.creadoEn = ahora;
        this.contrasenaCambiadaEn = ahora;
    }

    /** Emails sin distinguir mayúsculas ni espacios alrededor (FR-005). */
    public static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    public void verificar(Instant ahora) {
        if (estado == EstadoUsuario.PENDIENTE_VERIFICACION) {
            estado = EstadoUsuario.ACTIVO;
            verificadoEn = ahora;
        }
    }

    /**
     * Cuenta un intento fallido. Durante un bloqueo no cuenta ni lo amplía.
     *
     * @return {@code true} si este fallo acaba de bloquear la cuenta
     */
    public boolean registrarFallo(Instant ahora, int maxIntentos, Duration bloqueo) {
        if (estaBloqueada(ahora)) {
            return false;
        }
        intentosFallidos++;
        if (intentosFallidos < maxIntentos) {
            return false;
        }
        bloqueadoHasta = ahora.plus(bloqueo);
        intentosFallidos = 0;
        return true;
    }

    public void registrarExito() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public boolean estaBloqueada(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    public void cambiarHashContrasena(String hashContrasena, Instant ahora) {
        this.hashContrasena = hashContrasena;
        this.contrasenaCambiadaEn = ahora;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = normalizarEmail(email);
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getVerificadoEn() {
        return verificadoEn;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public Instant getContrasenaCambiadaEn() {
        return contrasenaCambiadaEn;
    }
}
