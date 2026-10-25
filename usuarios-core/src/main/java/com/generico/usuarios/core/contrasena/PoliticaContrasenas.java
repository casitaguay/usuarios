package com.generico.usuarios.core.contrasena;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.generico.usuarios.core.config.UsuariosProperties;

/**
 * Política de contraseñas (FR-023 a FR-025, research R10). Devuelve las claves de mensaje de los
 * requisitos incumplidos; una lista vacía significa que la contraseña es válida.
 */
@Component
public class PoliticaContrasenas {

    public static final String LONGITUD_MINIMA = "contrasena.longitud-minima";
    public static final String LONGITUD_MAXIMA = "contrasena.longitud-maxima";
    public static final String PROHIBIDA = "contrasena.prohibida";
    public static final String DATOS_PERSONALES = "contrasena.datos-personales";
    public static final String MAYUSCULA = "contrasena.mayuscula";
    public static final String CARACTER_ESPECIAL = "contrasena.caracter-especial";

    // Un nombre o parte local de 1 o 2 caracteres («Al», «x») rechazaría casi cualquier contraseña
    private static final int LONGITUD_MINIMA_DATO_PERSONAL = 3;

    private static final Set<String> PROHIBIDAS = cargarProhibidas();

    private final UsuariosProperties.Contrasena reglas;

    @Autowired
    public PoliticaContrasenas(UsuariosProperties propiedades) {
        this(propiedades.contrasena());
    }

    public PoliticaContrasenas(UsuariosProperties.Contrasena reglas) {
        this.reglas = reglas;
    }

    public UsuariosProperties.Contrasena reglas() {
        return reglas;
    }

    public List<String> validar(String contrasena, String email, String nombre) {
        String normalizada = Normalizer.normalize(contrasena, Normalizer.Form.NFKC);
        int longitud = normalizada.codePointCount(0, normalizada.length());
        List<String> incumplidos = new ArrayList<>();

        if (longitud < reglas.longitudMinima()) {
            incumplidos.add(LONGITUD_MINIMA);
        }
        if (longitud > reglas.longitudMaxima()) {
            incumplidos.add(LONGITUD_MAXIMA);
        }
        String minusculas = normalizada.toLowerCase(Locale.ROOT);
        if (PROHIBIDAS.contains(minusculas)) {
            incumplidos.add(PROHIBIDA);
        }
        if (contiene(minusculas, parteLocal(email)) || contiene(minusculas, nombre)) {
            incumplidos.add(DATOS_PERSONALES);
        }
        if (reglas.exigirMayuscula() && normalizada.codePoints().noneMatch(Character::isUpperCase)) {
            incumplidos.add(MAYUSCULA);
        }
        if (reglas.exigirCaracterEspecial() && normalizada.codePoints().allMatch(Character::isLetterOrDigit)) {
            incumplidos.add(CARACTER_ESPECIAL);
        }
        return incumplidos;
    }

    private static String parteLocal(String email) {
        if (email == null) {
            return null;
        }
        int arroba = email.indexOf('@');
        return arroba < 0 ? email : email.substring(0, arroba);
    }

    private static boolean contiene(String contrasena, String dato) {
        if (dato == null) {
            return false;
        }
        String normalizado = Normalizer.normalize(dato.strip(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        return normalizado.codePointCount(0, normalizado.length()) >= LONGITUD_MINIMA_DATO_PERSONAL
                && contrasena.contains(normalizado);
    }

    private static Set<String> cargarProhibidas() {
        var recurso = PoliticaContrasenas.class.getResourceAsStream("/contrasenas-prohibidas.txt");
        if (recurso == null) {
            throw new IllegalStateException("Falta el recurso contrasenas-prohibidas.txt");
        }
        try (var lector = new BufferedReader(new InputStreamReader(recurso, StandardCharsets.UTF_8))) {
            return lector.lines()
                    .filter(linea -> !linea.isBlank() && !linea.startsWith("#"))
                    .map(linea -> linea.strip().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
