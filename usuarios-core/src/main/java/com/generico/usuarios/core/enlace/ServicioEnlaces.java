package com.generico.usuarios.core.enlace;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.generico.usuarios.core.dominio.EnlaceUnUso;
import com.generico.usuarios.core.dominio.FinalidadEnlace;
import com.generico.usuarios.core.dominio.Usuario;
import com.generico.usuarios.core.repositorio.EnlaceUnUsoRepositorio;

/** Enlaces de un solo uso (research R7): 256 bits aleatorios, solo su SHA-256 en base de datos. */
@Service
public class ServicioEnlaces {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final EnlaceUnUsoRepositorio repositorio;
    private final Clock reloj;

    public ServicioEnlaces(EnlaceUnUsoRepositorio repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /** Emite un enlace nuevo, invalida los vigentes de la misma finalidad y devuelve el valor a enviar. */
    @Transactional
    public String emitir(Usuario usuario, FinalidadEnlace finalidad, Duration validez) {
        Instant ahora = reloj.instant();
        repositorio.invalidarVigentes(usuario, finalidad, ahora);
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repositorio.save(new EnlaceUnUso(usuario, finalidad, hash(valor), ahora, ahora.plus(validez)));
        return valor;
    }

    /** Consume el enlace si es válido; un valor caducado, usado, invalidado o inventado da vacío. */
    @Transactional
    public Optional<Usuario> consumir(String valor, FinalidadEnlace finalidad) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String hash = hash(valor);
        if (repositorio.consumir(hash, finalidad, reloj.instant()) != 1) {
            return Optional.empty();
        }
        return repositorio.findByHashValorAndFinalidad(hash, finalidad).map(EnlaceUnUso::getUsuario);
    }

    /** Titular de un enlace válido, sin consumirlo (para mostrar un formulario antes de usarlo). */
    @Transactional(readOnly = true)
    public Optional<Usuario> titular(String valor, FinalidadEnlace finalidad) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        return repositorio.findByHashValorAndFinalidad(hash(valor), finalidad)
                .filter(enlace -> enlace.esValido(reloj.instant()))
                .map(EnlaceUnUso::getUsuario);
    }

    static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
