package com.generico.usuarios.core.repositorio;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.generico.usuarios.core.dominio.EstadoUsuario;
import com.generico.usuarios.core.dominio.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {

    /** El email debe venir normalizado con {@link Usuario#normalizarEmail(String)}. */
    Optional<Usuario> findByEmail(String email);

    /** Consentimientos y enlaces se borran en cascada en la base de datos. */
    long deleteByEstadoAndCreadoEnBefore(EstadoUsuario estado, Instant limite);
}
