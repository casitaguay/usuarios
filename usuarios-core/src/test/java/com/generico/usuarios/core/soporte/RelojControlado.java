package com.generico.usuarios.core.soporte;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/** Reloj de tests: parado en un instante que el test fija o avanza. */
public class RelojControlado extends Clock {

    public static final Instant INICIO = Instant.parse("2026-01-01T10:00:00Z");

    private final AtomicReference<Instant> ahora = new AtomicReference<>(INICIO);

    public void fijar(Instant instante) {
        ahora.set(instante);
    }

    public void avanzar(Duration duracion) {
        ahora.updateAndGet(instante -> instante.plus(duracion));
    }

    public void reiniciar() {
        fijar(INICIO);
    }

    @Override
    public Instant instant() {
        return ahora.get();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        throw new UnsupportedOperationException("El reloj de tests siempre es UTC");
    }
}
