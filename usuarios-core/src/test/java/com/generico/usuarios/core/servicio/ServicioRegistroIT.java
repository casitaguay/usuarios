package com.generico.usuarios.core.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.generico.usuarios.core.contrasena.ContrasenaNoValidaException;
import com.generico.usuarios.core.contrasena.PoliticaContrasenas;
import com.generico.usuarios.core.dominio.Consentimiento;
import com.generico.usuarios.core.dominio.EstadoUsuario;
import com.generico.usuarios.core.dominio.TipoDocumento;
import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.repositorio.ConsentimientoRepositorio;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;
import com.generico.usuarios.core.soporte.BaseIT;

class ServicioRegistroIT extends BaseIT {

    static final String CONTRASENA = "caballo grapa bateria";

    @Autowired
    ServicioRegistro registro;

    @Autowired
    UsuarioRepositorio usuarios;

    @Autowired
    ConsentimientoRepositorio consentimientos;

    @Test
    void elAltaCreaUnaCuentaPendienteConConsentimientosYEnviaLaVerificacion() {
        registro.registrar("Ana", "ana@ejemplo.com", CONTRASENA);

        Usuario usuario = usuarios.findByEmail("ana@ejemplo.com").orElseThrow();
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE_VERIFICACION);
        assertThat(usuario.getHashContrasena()).startsWith("{argon2}").doesNotContain(CONTRASENA);
        assertThat(usuario.getCreadoEn()).isEqualTo(reloj.instant());

        assertThat(consentimientos.findByUsuarioId(usuario.getId()))
                .extracting(Consentimiento::getTipoDocumento, Consentimiento::getVersionDocumento, Consentimiento::getAceptadoEn)
                .containsExactlyInAnyOrder(
                        tuple(TipoDocumento.CONDICIONES_USO, "1.0-ejemplo", reloj.instant()),
                        tuple(TipoDocumento.POLITICA_PRIVACIDAD, "1.0-ejemplo", reloj.instant()));

        assertThat(emails.enviados()).singleElement().satisfies(mensaje -> {
            assertThat(mensaje.plantilla()).isEqualTo("verificacion");
            assertThat(mensaje.destinatario()).isEqualTo("ana@ejemplo.com");
            assertThat((String) mensaje.variables().get("enlace")).startsWith("http://localhost:8080/verificacion?t=");
        });
    }

    @Test
    void elEmailSeNormaliza() {
        registro.registrar("Ana", "  Ana@Ejemplo.COM ", CONTRASENA);

        assertThat(usuarios.findByEmail("ana@ejemplo.com")).isPresent();
    }

    @Test
    void unEmailExistenteNoCreaCuentaYAvisaAlTitular() {
        registro.registrar("Ana", "ana@ejemplo.com", CONTRASENA);
        emails.limpiar();

        registro.registrar("Otra persona", "ANA@ejemplo.com", "otra contrasena larga");

        assertThat(usuarios.count()).isEqualTo(1);
        assertThat(usuarios.findByEmail("ana@ejemplo.com").orElseThrow().getNombre()).isEqualTo("Ana");
        assertThat(emails.enviados()).singleElement().satisfies(mensaje -> {
            assertThat(mensaje.plantilla()).isEqualTo("registro-existente");
            assertThat(mensaje.destinatario()).isEqualTo("ana@ejemplo.com");
        });
    }

    @Test
    void unaContrasenaInvalidaNoCreaNadaNiEnviaEmails() {
        assertThatThrownBy(() -> registro.registrar("Ana", "ana@ejemplo.com", "corta"))
                .isInstanceOfSatisfying(ContrasenaNoValidaException.class,
                        e -> assertThat(e.getIncumplidos()).contains(PoliticaContrasenas.LONGITUD_MINIMA));

        assertThat(usuarios.count()).isZero();
        assertThat(consentimientos.count()).isZero();
        assertThat(emails.enviados()).isEmpty();
    }
}
