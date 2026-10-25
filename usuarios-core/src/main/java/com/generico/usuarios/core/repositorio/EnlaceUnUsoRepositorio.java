package com.generico.usuarios.core.repositorio;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.generico.usuarios.core.dominio.EnlaceUnUso;
import com.generico.usuarios.core.dominio.FinalidadEnlace;
import com.generico.usuarios.core.dominio.Usuario;

public interface EnlaceUnUsoRepositorio extends JpaRepository<EnlaceUnUso, UUID> {

    Optional<EnlaceUnUso> findByHashValorAndFinalidad(String hashValor, FinalidadEnlace finalidad);

    /** Consumo atómico: de dos usos simultáneos solo uno actualiza la fila (research R7). */
    @Modifying
    @Query("""
            update EnlaceUnUso e set e.usadoEn = :ahora
            where e.hashValor = :hash and e.finalidad = :finalidad
              and e.usadoEn is null and e.invalidadoEn is null and e.caducaEn > :ahora""")
    int consumir(@Param("hash") String hash, @Param("finalidad") FinalidadEnlace finalidad,
            @Param("ahora") Instant ahora);

    @Modifying
    @Query("""
            update EnlaceUnUso e set e.invalidadoEn = :ahora
            where e.usuario = :usuario and e.finalidad = :finalidad
              and e.usadoEn is null and e.invalidadoEn is null""")
    int invalidarVigentes(@Param("usuario") Usuario usuario, @Param("finalidad") FinalidadEnlace finalidad,
            @Param("ahora") Instant ahora);

    @Modifying
    @Query("delete from EnlaceUnUso e where e.usadoEn < :limite or e.invalidadoEn < :limite or e.caducaEn < :limite")
    int borrarObsoletos(@Param("limite") Instant limite);
}
