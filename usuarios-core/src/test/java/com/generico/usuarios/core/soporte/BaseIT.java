package com.generico.usuarios.core.soporte;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.generico.usuarios.core.email.NotificadorEmailEnMemoria;

/** Base de los tests de integración: PostgreSQL real ({@code usuarios_test}) y tablas vacías en cada test. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ConfiguracionTest.class)
public abstract class BaseIT {

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected RelojControlado reloj;

    @Autowired
    protected NotificadorEmailEnMemoria emails;

    @BeforeEach
    void limpiarEstado() {
        jdbc.execute("TRUNCATE usuario CASCADE");
        reloj.reiniciar();
        emails.limpiar();
    }
}
