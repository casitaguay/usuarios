package com.generico.usuarios.core.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionRegistryImpl;

class ServicioSesionesTest {

    private final SessionRegistryImpl registro = new SessionRegistryImpl();
    private final ServicioSesiones servicio = new ServicioSesiones(registro);

    private final UsuarioAutenticado ana = principal("ana@ejemplo.com");
    private final UsuarioAutenticado luis = principal("luis@ejemplo.com");

    @Test
    void expirarTodasSoloAfectaAlUsuarioIndicado() {
        registro.registerNewSession("a1", ana);
        registro.registerNewSession("a2", ana);
        registro.registerNewSession("l1", luis);

        servicio.expirarTodas(ana.getId());

        assertThat(registro.getSessionInformation("a1").isExpired()).isTrue();
        assertThat(registro.getSessionInformation("a2").isExpired()).isTrue();
        assertThat(registro.getSessionInformation("l1").isExpired()).isFalse();
    }

    @Test
    void expirarTodasSalvoConservaLaSesionActual() {
        registro.registerNewSession("a1", ana);
        registro.registerNewSession("a2", ana);

        servicio.expirarTodasSalvo(ana.getId(), "a1");

        assertThat(registro.getSessionInformation("a1").isExpired()).isFalse();
        assertThat(registro.getSessionInformation("a2").isExpired()).isTrue();
    }

    @Test
    void unaCuentaNuevaConElEmailDeOtraNoSeMezclaConSusSesiones() {
        var anterior = principal("ana@ejemplo.com");
        var nueva = principal("ana@ejemplo.com");
        registro.registerNewSession("vieja", anterior);
        registro.registerNewSession("nueva", nueva);

        servicio.expirarTodas(nueva.getId());

        assertThat(registro.getSessionInformation("nueva").isExpired()).isTrue();
        assertThat(registro.getSessionInformation("vieja").isExpired()).isFalse();
    }

    private static UsuarioAutenticado principal(String email) {
        return new UsuarioAutenticado(UUID.randomUUID(), email, "Nombre", "{argon2}hash");
    }
}
