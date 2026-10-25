package com.generico.usuarios.core.enlace;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.generico.usuarios.core.dominio.FinalidadEnlace;
import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;
import com.generico.usuarios.core.soporte.BaseIT;

class ServicioEnlacesIT extends BaseIT {

    private static final Duration VALIDEZ = Duration.ofHours(1);
    private static final FinalidadEnlace RECUPERACION = FinalidadEnlace.RECUPERACION_CONTRASENA;

    @Autowired
    ServicioEnlaces enlaces;

    @Autowired
    UsuarioRepositorio usuarios;

    private Usuario usuario;

    @BeforeEach
    void crearUsuario() {
        usuario = usuarios.save(new Usuario("Ana", "ana@ejemplo.com", "{argon2}hash", reloj.instant()));
    }

    @Test
    void enBaseDeDatosSoloEstaElHash() {
        String valor = enlaces.emitir(usuario, RECUPERACION, VALIDEZ);

        assertThat(valor).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(jdbc.queryForList("SELECT hash_valor FROM enlace_un_uso", String.class))
                .containsExactly(ServicioEnlaces.hash(valor))
                .doesNotContain(valor);
    }

    @Test
    void soloSePuedeConsumirUnaVez() {
        String valor = enlaces.emitir(usuario, RECUPERACION, VALIDEZ);

        assertThat(enlaces.consumir(valor, RECUPERACION)).map(Usuario::getId).contains(usuario.getId());
        assertThat(enlaces.consumir(valor, RECUPERACION)).isEmpty();
    }

    @Test
    void caducadoManipuladoODeOtraFinalidadNoSeConsume() {
        String valor = enlaces.emitir(usuario, RECUPERACION, VALIDEZ);

        assertThat(enlaces.consumir(valor + "x", RECUPERACION)).isEmpty();
        assertThat(enlaces.consumir("inventado", RECUPERACION)).isEmpty();
        assertThat(enlaces.consumir(null, RECUPERACION)).isEmpty();
        assertThat(enlaces.consumir(valor, FinalidadEnlace.VERIFICACION_EMAIL)).isEmpty();

        reloj.avanzar(VALIDEZ);
        assertThat(enlaces.consumir(valor, RECUPERACION)).isEmpty();
    }

    @Test
    void emitirUnoNuevoInvalidaLosAnterioresDeLaMismaFinalidad() {
        String anterior = enlaces.emitir(usuario, RECUPERACION, VALIDEZ);
        String verificacion = enlaces.emitir(usuario, FinalidadEnlace.VERIFICACION_EMAIL, VALIDEZ);
        String nuevo = enlaces.emitir(usuario, RECUPERACION, VALIDEZ);

        assertThat(enlaces.consumir(anterior, RECUPERACION)).isEmpty();
        assertThat(enlaces.consumir(nuevo, RECUPERACION)).isPresent();
        assertThat(enlaces.consumir(verificacion, FinalidadEnlace.VERIFICACION_EMAIL)).isPresent();
    }

    @Test
    void dosConsumosSimultaneosDanUnSoloExito() throws Exception {
        String valor = enlaces.emitir(usuario, RECUPERACION, VALIDEZ);
        var salida = new CountDownLatch(1);
        Callable<Boolean> consumir = () -> {
            salida.await();
            return enlaces.consumir(valor, RECUPERACION).isPresent();
        };

        try (var hilos = Executors.newFixedThreadPool(2)) {
            Future<Boolean> primero = hilos.submit(consumir);
            Future<Boolean> segundo = hilos.submit(consumir);
            salida.countDown();
            assertThat(primero.get() ^ segundo.get()).as("exactamente un consumo con éxito").isTrue();
        }
    }
}
