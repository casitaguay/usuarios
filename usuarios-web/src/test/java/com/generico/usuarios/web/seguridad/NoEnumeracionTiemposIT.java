package com.generico.usuarios.web.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;
import com.generico.usuarios.core.soporte.BaseIT;

/**
 * SC-003: el tiempo de respuesta no revela si un email tiene cuenta. Para cada envío, 50
 * peticiones con un email existente y 50 con uno inexistente, tras un calentamiento; la diferencia
 * de las medias debe ser inferior a 50 ms.
 */
class NoEnumeracionTiemposIT extends BaseIT {

    private static final int CALENTAMIENTO = 10;
    private static final int MUESTRAS = 50;
    private static final Duration DIFERENCIA_MAXIMA = Duration.ofMillis(50);
    private static final String CONTRASENA = "caballo grapa bateria";

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepositorio usuarios;

    @Autowired
    PasswordEncoder codificador;

    /** Cada petición sale de una IP distinta para no activar la limitación por origen. */
    private final AtomicInteger ip = new AtomicInteger();

    @BeforeEach
    void crearCuentas() {
        String hash = codificador.encode(CONTRASENA);
        var activa = new Usuario("Ana", "existe@ejemplo.com", hash, reloj.instant());
        activa.verificar(reloj.instant());
        usuarios.save(activa);
        usuarios.save(new Usuario("Luis", "pendiente@ejemplo.com", hash, reloj.instant()));
    }

    @Test
    void inicioDeSesion() throws Exception {
        comparar(i -> post("/acceso").param("email", "existe@ejemplo.com").param("contrasena", "contrasena equivocada"),
                i -> post("/acceso").param("email", "nadie@ejemplo.com").param("contrasena", "contrasena equivocada"));
    }

    @Test
    void registro() throws Exception {
        comparar(i -> registro("existe@ejemplo.com"), i -> registro("nuevo" + i + "@ejemplo.com"));
    }

    @Test
    void recuperacion() throws Exception {
        comparar(i -> post("/recuperacion").param("email", "existe@ejemplo.com"),
                i -> post("/recuperacion").param("email", "nadie@ejemplo.com"));
    }

    @Test
    void reenvioDeVerificacion() throws Exception {
        comparar(i -> post("/verificacion/reenvio").param("email", "pendiente@ejemplo.com"),
                i -> post("/verificacion/reenvio").param("email", "nadie@ejemplo.com"));
    }

    private void comparar(IntFunction<MockHttpServletRequestBuilder> conCuenta,
            IntFunction<MockHttpServletRequestBuilder> sinCuenta) throws Exception {
        for (int i = 0; i < CALENTAMIENTO; i++) {
            medir(conCuenta.apply(-i));
            medir(sinCuenta.apply(-i - 1000));
        }
        long totalCon = 0;
        long totalSin = 0;
        for (int i = 0; i < MUESTRAS; i++) {
            // Alternadas, para que una pausa del recolector de basura no penalice solo a un caso
            totalCon += medir(conCuenta.apply(i));
            totalSin += medir(sinCuenta.apply(i));
        }
        long diferencia = Math.abs(totalCon - totalSin) / MUESTRAS;
        assertThat(Duration.ofNanos(diferencia)).isLessThan(DIFERENCIA_MAXIMA);
    }

    private long medir(MockHttpServletRequestBuilder peticion) throws Exception {
        String origen = "10.1." + (ip.get() / 250) + "." + (ip.getAndIncrement() % 250 + 1);
        long inicio = System.nanoTime();
        mvc.perform(peticion.with(csrf()).with(p -> {
            p.setRemoteAddr(origen);
            return p;
        }));
        return System.nanoTime() - inicio;
    }

    private static MockHttpServletRequestBuilder registro(String email) {
        return post("/registro").param("nombre", "Persona").param("email", email).param("contrasena", CONTRASENA)
                .param("aceptaCondiciones", "true").param("aceptaPrivacidad", "true");
    }
}
