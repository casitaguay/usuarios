package com.generico.usuarios.core.repositorio;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.generico.usuarios.core.dominio.EstadoUsuario;
import com.generico.usuarios.core.dominio.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {

    /** El email debe venir normalizado con {@link Usuario#normalizarEmail(String)}. */
    Optional<Usuario> findByEmail(String email);

    /** Bloquea la fila: intentos de acceso simultáneos cuentan todos sin conflictos de versión. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Usuario> findParaActualizarByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Usuario> findParaActualizarById(UUID id);

    /** Consentimientos y enlaces se borran en cascada en la base de datos. */
    long deleteByEstadoAndCreadoEnBefore(EstadoUsuario estado, Instant limite);
}
