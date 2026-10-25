package com.generico.usuarios.web.rendimiento;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;
import com.generico.usuarios.core.soporte.ConfiguracionTest;

/**
 * SC-009 (research R20): con 100 sesiones simultáneas durante un minuto, el 95% de las páginas
 * responde en menos de 2 segundos. Solo se ejecuta con {@code ./mvnw verify -Prendimiento}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "usuarios.limite-peticiones.por-minuto=100000")
@ActiveProfiles("test")
@Import(ConfiguracionTest.class)
class RendimientoIT {

    private static final int SESIONES = 100;
    private static final Duration DURACION = Duration.ofSeconds(60);
    private static final Duration PERCENTIL_95_MAXIMO = Duration.ofSeconds(2);
    private static final String CONTRASENA = "caballo grapa bateria";
    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
    private static final Pattern COOKIE = Pattern.compile("USUARIOS_SESION=([^;]+)");

    @LocalServerPort
    int puerto;

    @Autowired
    UsuarioRepositorio usuarios;

    @Autowired
    PasswordEncoder codificador;

    @Autowired
    JdbcTemplate jdbc;

    // Sin redirecciones automáticas y con la cookie a mano: es Secure y HttpClient no la envía por HTTP
    private final HttpClient cliente = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @Test
    void elPercentil95EsInferiorADosSegundos() throws Exception {
        jdbc.execute("TRUNCATE usuario CASCADE");
        String hash = codificador.encode(CONTRASENA);
        for (int i = 0; i < SESIONES; i++) {
            var usuario = new Usuario("Persona " + i, "persona" + i + "@ejemplo.com", hash, Instant.now());
            usuario.verificar(Instant.now());
            usuarios.save(usuario);
        }
        // Los inicios de sesión van de uno en uno: Argon2 usa 19 MiB por cálculo
        List<String> cookies = new ArrayList<>();
        for (int i = 0; i < SESIONES; i++) {
            cookies.add(iniciarSesion("persona" + i + "@ejemplo.com"));
        }

        List<Long> tiempos = Collections.synchronizedList(new ArrayList<>());
        long fin = System.nanoTime() + DURACION.toNanos();
        List<Future<Object>> sesiones = new ArrayList<>();
        try (var hilos = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String cookie : cookies) {
                sesiones.add(hilos.submit(() -> {
                    while (System.nanoTime() < fin) {
                        for (String ruta : List.of("/inicio", "/cuenta/contrasena")) {
                            long inicio = System.nanoTime();
                            var respuesta = cliente.send(peticion(ruta, cookie).GET().build(), HttpResponse.BodyHandlers.discarding());
                            tiempos.add(System.nanoTime() - inicio);
                            assertThat(respuesta.statusCode()).as(ruta).isEqualTo(200);
                        }
                    }
                    return null;
                }));
            }
        }
        for (var sesion : sesiones) {
            sesion.get(); // propaga cualquier respuesta distinta de 200
        }

        List<Long> ordenados = new ArrayList<>(tiempos);
        Collections.sort(ordenados);
        Duration percentil95 = Duration.ofNanos(ordenados.get((int) Math.ceil(ordenados.size() * 0.95) - 1));
        System.out.printf("Rendimiento: %d peticiones, percentil 95 = %d ms%n", ordenados.size(), percentil95.toMillis());
        assertThat(percentil95).isLessThan(PERCENTIL_95_MAXIMO);
    }

    private String iniciarSesion(String email) throws Exception {
        var formulario = cliente.send(peticion("/acceso", null).GET().build(), HttpResponse.BodyHandlers.ofString());
        String cookie = cookie(formulario, null);
        var token = CSRF.matcher(formulario.body());
        assertThat(token.find()).as("token CSRF en /acceso").isTrue();
        String cuerpo = "email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&contrasena=" + URLEncoder.encode(CONTRASENA, StandardCharsets.UTF_8)
                + "&_csrf=" + URLEncoder.encode(token.group(1), StandardCharsets.UTF_8);
        var acceso = cliente.send(peticion("/acceso", cookie)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(), HttpResponse.BodyHandlers.discarding());
        assertThat(acceso.headers().firstValue("Location")).as("acceso de " + email).hasValueSatisfying(
                destino -> assertThat(destino).endsWith("/inicio"));
        return cookie(acceso, cookie);
    }

    private HttpRequest.Builder peticion(String ruta, String cookie) {
        var constructor = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta));
        return cookie == null ? constructor : constructor.header("Cookie", "USUARIOS_SESION=" + cookie);
    }

    /** Nueva cookie de sesión si la respuesta la cambia (al iniciar sesión cambia el identificador). */
    private static String cookie(HttpResponse<?> respuesta, String anterior) {
        return respuesta.headers().allValues("Set-Cookie").stream()
                .map(COOKIE::matcher).filter(Matcher::find).map(m -> m.group(1))
                .findFirst().orElse(anterior);
    }
}
