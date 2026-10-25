package com.generico.usuarios.core.dominio;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Credencial temporal de un solo uso. Solo se guarda el hash del valor enviado (FR-030). */
@Entity
@Table(name = "enlace_un_uso")
public class EnlaceUnUso {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private FinalidadEnlace finalidad;

    @Column(name = "hash_valor", columnDefinition = "char(64)")
    private String hashValor;

    @Column(name = "creado_en")
    private Instant creadoEn;

    @Column(name = "caduca_en")
    private Instant caducaEn;

    @Column(name = "usado_en")
    private Instant usadoEn;

    @Column(name = "invalidado_en")
    private Instant invalidadoEn;

    protected EnlaceUnUso() {
    }

    public EnlaceUnUso(Usuario usuario, FinalidadEnlace finalidad, String hashValor, Instant creadoEn, Instant caducaEn) {
        this.id = UUID.randomUUID();
        this.usuario = usuario;
        this.finalidad = finalidad;
        this.hashValor = hashValor;
        this.creadoEn = creadoEn;
        this.caducaEn = caducaEn;
    }

    public boolean esValido(Instant ahora) {
        return usadoEn == null && invalidadoEn == null && caducaEn.isAfter(ahora);
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public FinalidadEnlace getFinalidad() {
        return finalidad;
    }

    public String getHashValor() {
        return hashValor;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getCaducaEn() {
        return caducaEn;
    }

    public Instant getUsadoEn() {
        return usadoEn;
    }

    public Instant getInvalidadoEn() {
        return invalidadoEn;
    }
}
