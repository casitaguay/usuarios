package com.generico.usuarios.core.repositorio;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.generico.usuarios.core.dominio.Consentimiento;

public interface ConsentimientoRepositorio extends JpaRepository<Consentimiento, UUID> {

    List<Consentimiento> findByUsuarioId(UUID usuarioId);
}
