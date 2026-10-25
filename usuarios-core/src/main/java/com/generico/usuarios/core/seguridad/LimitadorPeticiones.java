package com.generico.usuarios.core.seguridad;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.generico.usuarios.core.config.UsuariosProperties;

/**
 * Ventana fija de un minuto por IP de origen y grupo de rutas (research R9). Al superar el límite,
 * el origen queda frenado durante el tiempo configurado.
 */
// shortcut: en memoria y por instancia (SC-010); con varias instancias, el límite global va en el proxy inverso
@Component
public class LimitadorPeticiones {

    private static final Duration VENTANA = Duration.ofMinutes(1);

    private record Clave(String origen, String grupo) {
    }

    private record Estado(Instant inicioVentana, int peticiones, Instant frenadoHasta) {

        boolean caducado(Instant ahora) {
            Instant fin = frenadoHasta != null ? frenadoHasta : inicioVentana.plus(VENTANA);
            return !ahora.isBefore(fin);
        }
    }

    private final Map<Clave, Estado> estados = new ConcurrentHashMap<>();
    private final Clock reloj;
    private final UsuariosProperties.LimitePeticiones configuracion;

    @Autowired
    public LimitadorPeticiones(Clock reloj, UsuariosProperties propiedades) {
        this(reloj, propiedades.limitePeticiones());
    }

    public LimitadorPeticiones(Clock reloj, UsuariosProperties.LimitePeticiones configuracion) {
        this.reloj = reloj;
        this.configuracion = configuracion;
    }

    /** Cuenta la petición y dice si se permite. */
    public boolean permitir(String origen, String grupo) {
        Instant ahora = reloj.instant();
        Estado estado = estados.compute(new Clave(origen, grupo), (clave, actual) -> {
            if (actual != null && actual.frenadoHasta() != null && !actual.caducado(ahora)) {
                return actual;
            }
            Estado base = actual == null || actual.caducado(ahora) ? new Estado(ahora, 0, null) : actual;
            int peticiones = base.peticiones() + 1;
            Instant frenadoHasta = peticiones > configuracion.porMinuto() ? ahora.plus(configuracion.freno()) : null;
            return new Estado(base.inicioVentana(), peticiones, frenadoHasta);
        });
        return estado.frenadoHasta() == null;
    }

    /**
     * IP de origen. {@code X-Forwarded-For} solo se tiene en cuenta si la conexión llega de un proxy
     * de confianza; entonces se toma la última IP de la cabecera que no sea de un proxy de confianza.
     */
    public String origen(HttpServletRequest peticion) {
        String remota = peticion.getRemoteAddr();
        List<String> proxies = configuracion.proxiesConfianza();
        String cabecera = peticion.getHeader("X-Forwarded-For");
        if (cabecera == null || !proxies.contains(remota)) {
            return remota;
        }
        String[] saltos = cabecera.split(",");
        for (int i = saltos.length - 1; i >= 0; i--) {
            String ip = saltos[i].strip();
            if (!ip.isEmpty() && !proxies.contains(ip)) {
                return ip;
            }
        }
        return remota;
    }

    @Scheduled(fixedRate = 60_000)
    public void limpiar() {
        Instant ahora = reloj.instant();
        estados.values().removeIf(estado -> estado.caducado(ahora));
    }

    /** Vacía todos los contadores (para tests). */
    public void reiniciar() {
        estados.clear();
    }

    int entradas() {
        return estados.size();
    }
}
