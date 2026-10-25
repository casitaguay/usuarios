package com.generico.usuarios.core.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.generico.usuarios.core.contrasena.ContrasenaNoValidaException;
import com.generico.usuarios.core.contrasena.PoliticaContrasenas;
import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;
import com.generico.usuarios.core.seguridad.ServicioSesiones;
import com.generico.usuarios.core.soporte.BaseIT;

class ServicioCambioContrasenaIT extends BaseIT {

    private static final String ACTUAL = "caballo grapa bateria";
    private static final String NUEVA = "otra frase bastante larga";
    private static final String SESION = "sesion-actual";

    @Autowired
    ServicioCambioContrasena cambio;

    @Autowired
    UsuarioRepositorio usuarios;

    @Autowired
    PasswordEncoder codificador;

    @MockitoBean
    ServicioSesiones sesiones;

    private Usuario usuario;

    @BeforeEach
    void crearCuenta() {
        usuario = new Usuario("Ana Pruebas", "ana@ejemplo.com", codificador.encode(ACTUAL), reloj.instant());
        usuario.verificar(reloj.instant());
        usuario = usuarios.save(usuario);
    }

    @Test
    void conLaActualCorrectaCambiaLaContrasenaCierraLasDemasSesionesYAvisa() {
        cambio.cambiar(usuario.getId(), ACTUAL, NUEVA, SESION);

        assertThat(codificador.matches(NUEVA, recargar().getHashContrasena())).isTrue();
        verify(sesiones).expirarTodasSalvo(usuario.getId(), SESION);
        assertThat(emails.enviados("contrasena-cambiada")).singleElement()
                .satisfies(m -> assertThat(m.destinatario()).isEqualTo("ana@ejemplo.com"));
    }

    @Test
    void conLaActualIncorrectaNoCambiaNadaYCuentaComoFallo() {
        assertThatThrownBy(() -> cambio.cambiar(usuario.getId(), "contrasena equivocada", NUEVA, SESION))
                .isInstanceOf(ContrasenaActualIncorrectaException.class);

        Usuario actualizado = recargar();
        assertThat(codificador.matches(ACTUAL, actualizado.getHashContrasena())).isTrue();
        assertThat(actualizado.getIntentosFallidos()).isEqualTo(1);
        verify(sesiones, never()).expirarTodasSalvo(any(), anyString());
        assertThat(emails.enviados()).isEmpty();
    }

    @Test
    void elQuintoFalloBloqueaLaCuentaYAvisa() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> cambio.cambiar(usuario.getId(), "contrasena equivocada", NUEVA, SESION))
                    .isInstanceOf(ContrasenaActualIncorrectaException.class);
        }

        assertThat(recargar().estaBloqueada(reloj.instant())).isTrue();
        assertThat(emails.enviados("cuenta-bloqueada")).hasSize(1);
    }

    @Test
    void unaNuevaIgualALaActualOQueIncumpleLaPoliticaSeRechaza() {
        assertThatThrownBy(() -> cambio.cambiar(usuario.getId(), ACTUAL, ACTUAL, SESION))
                .isInstanceOfSatisfying(ContrasenaNoValidaException.class,
                        e -> assertThat(e.getIncumplidos()).containsExactly(ServicioCambioContrasena.IGUAL_A_LA_ACTUAL));
        assertThatThrownBy(() -> cambio.cambiar(usuario.getId(), ACTUAL, "corta", SESION))
                .isInstanceOfSatisfying(ContrasenaNoValidaException.class,
                        e -> assertThat(e.getIncumplidos()).contains(PoliticaContrasenas.LONGITUD_MINIMA));

        assertThat(codificador.matches(ACTUAL, recargar().getHashContrasena())).isTrue();
        assertThat(recargar().getIntentosFallidos()).isZero();
    }

    private Usuario recargar() {
        return usuarios.findById(usuario.getId()).orElseThrow();
    }
}
