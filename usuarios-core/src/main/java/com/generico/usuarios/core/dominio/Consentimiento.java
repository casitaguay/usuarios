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

/** Aceptación de un texto legal con su versión y fecha (FR-003). Solo inserción. */
@Entity
@Table(name = "consentimiento")
public class Consentimiento {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento")
    private TipoDocumento tipoDocumento;

    @Column(name = "version_documento")
    private String versionDocumento;

    @Column(name = "aceptado_en")
    private Instant aceptadoEn;

    protected Consentimiento() {
    }

    public Consentimiento(Usuario usuario, TipoDocumento tipoDocumento, String versionDocumento, Instant aceptadoEn) {
        this.id = UUID.randomUUID();
        this.usuario = usuario;
        this.tipoDocumento = tipoDocumento;
        this.versionDocumento = versionDocumento;
        this.aceptadoEn = aceptadoEn;
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public String getVersionDocumento() {
        return versionDocumento;
    }

    public Instant getAceptadoEn() {
        return aceptadoEn;
    }
}
