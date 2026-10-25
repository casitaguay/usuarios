package com.generico.usuarios.core.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.generico.usuarios.core.config.UsuariosProperties;
import com.generico.usuarios.core.dominio.EstadoUsuario;
import com.generico.usuarios.core.repositorio.EnlaceUnUsoRepositorio;
import com.generico.usuarios.core.repositorio.UsuarioRepositorio;

/**
 * Tarea diaria (research R14): borra las cuentas sin verificar pasado el plazo, con sus
 * consentimientos y enlaces en cascada (FR-010), y los enlaces que ya no sirven.
 */
@Component
public class LimpiezaCuentasPendientes {

    private static final Logger LOG = LoggerFactory.getLogger(LimpiezaCuentasPendientes.class);

    /** Plazo de conservación de enlaces usados, invalidados o caducados (data-model.md). */
    private static final Duration CONSERVACION_ENLACES = Duration.ofDays(7);

    private final UsuarioRepositorio usuarios;
    private final EnlaceUnUsoRepositorio enlaces;
    private final UsuariosProperties propiedades;
    private final Clock reloj;

    public LimpiezaCuentasPendientes(UsuarioRepositorio usuarios, EnlaceUnUsoRepositorio enlaces,
            UsuariosProperties propiedades, Clock reloj) {
        this.usuarios = usuarios;
        this.enlaces = enlaces;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    // El valor por defecto se repite aquí porque el marcador no ve los de UsuariosProperties
    @Scheduled(cron = "${usuarios.limpieza.cron:0 0 3 * * *}")
    @Transactional
    public void limpiar() {
        Instant ahora = reloj.instant();
        long cuentas = usuarios.deleteByEstadoAndCreadoEnBefore(EstadoUsuario.PENDIENTE_VERIFICACION,
                ahora.minus(propiedades.verificacion().borrarPendientesTras()));
        int borrados = enlaces.borrarObsoletos(ahora.minus(CONSERVACION_ENLACES));
        LOG.info("Limpieza diaria: {} cuentas sin verificar y {} enlaces obsoletos borrados", cuentas, borrados);
    }
}
