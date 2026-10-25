package com.generico.usuarios.core.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.generico.usuarios.core.dominio.FinalidadEnlace;
import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.enlace.ServicioEnlaces;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;
import com.generico.usuarios.core.soporte.BaseIT;
import com.generico.usuarios.core.soporte.RelojControlado;

class LimpiezaCuentasIT extends BaseIT {

    @Autowired
    LimpiezaCuentasPendientes limpieza;

    @Autowired
    UsuarioRepositorio usuarios;

    @Autowired
    ServicioEnlaces enlaces;

    @Test
    void borraLasPendientesDeMasDeSieteDiasConSusConsentimientosYEnlaces() {
        Usuario antigua = crear("antigua@ejemplo.com", Duration.ofDays(7).plusSeconds(1), false);
        enlaces.emitir(antigua, FinalidadEnlace.VERIFICACION_EMAIL, Duration.ofHours(24));
        jdbc.update("INSERT INTO consentimiento VALUES (gen_random_uuid(), ?, 'CONDICIONES_USO', '1', now())", antigua.getId());
        crear("reciente@ejemplo.com", Duration.ofDays(6), false);
        crear("activa@ejemplo.com", Duration.ofDays(30), true);

        reloj.reiniciar();
        limpieza.limpiar();

        assertThat(usuarios.findAll()).extracting(Usuario::getEmail)
                .containsExactlyInAnyOrder("reciente@ejemplo.com", "activa@ejemplo.com");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM consentimiento", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM enlace_un_uso", Integer.class)).isZero();
    }

    @Test
    void borraLosEnlacesUsadosInvalidadosOCaducadosHaceMasDeSieteDias() {
        Usuario usuario = crear("ana@ejemplo.com", Duration.ofDays(10), true);
        reloj.fijar(RelojControlado.INICIO.minus(Duration.ofDays(9)));
        enlaces.emitir(usuario, FinalidadEnlace.RECUPERACION_CONTRASENA, Duration.ofHours(1)); // invalidado por el siguiente
        String usado = enlaces.emitir(usuario, FinalidadEnlace.RECUPERACION_CONTRASENA, Duration.ofHours(1));
        enlaces.consumir(usado, FinalidadEnlace.RECUPERACION_CONTRASENA);
        enlaces.emitir(usuario, FinalidadEnlace.VERIFICACION_EMAIL, Duration.ofHours(1)); // caduca sin usarse
        reloj.reiniciar();
        enlaces.emitir(usuario, FinalidadEnlace.VERIFICACION_EMAIL, Duration.ofHours(1)); // vigente

        limpieza.limpiar();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM enlace_un_uso", Integer.class)).isEqualTo(1);
    }

    private Usuario crear(String email, Duration antiguedad, boolean verificada) {
        reloj.fijar(RelojControlado.INICIO.minus(antiguedad));
        var usuario = new Usuario("Nombre", email, "{argon2}hash", reloj.instant());
        if (verificada) {
            usuario.verificar(reloj.instant());
        }
        return usuarios.save(usuario);
    }
}
