package io.github.juand13go.cartera.adaptadores.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comprueba que Flyway crea el esquema y que la base de datos protege los datos por sí misma.
 * Cada prueba corre en una transacción que se deshace al final, así no se ensucian las demás.
 */
@SpringBootTest
@Transactional
class EsquemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    private void insertarCliente(String cedula) {
        jdbc.update("INSERT INTO cliente (cedula, nombre, telefono, cupo_maximo, pin_cifrado) VALUES (?, ?, ?, ?, ?)",
                cedula, "Cliente de prueba", "3001234567", 5000000, "hash");
    }

    private long insertarPrestamo(String cedula) {
        jdbc.update("""
                INSERT INTO prestamo (cedula_cliente, capital, tasa_por_periodo, base_interes, fecha_desembolso,
                    frecuencia, dia_cobro, calendario, politica_mora, valor_mora, dias_gracia,
                    orden_imputacion, al_vencer_plazo, interes_cancelacion)
                VALUES (?, 2000000, 8, 'CAPITAL_INICIAL', DATE '2026-11-10', 'MENSUAL', 10, 'TODOS_LOS_DIAS',
                    'SIN_MULTA', NULL, 2, 'MULTAS_PRIMERO', 'TRATAR_COMO_MORA', 'PERIODO_COMPLETO')
                """, cedula);
        return jdbc.queryForObject("SELECT MAX(id) FROM prestamo WHERE cedula_cliente = ?", Long.class, cedula);
    }

    @Test
    void flywayCreaLasCuatroTablas() {
        for (String tabla : new String[] {"cliente", "prestamo", "pago", "condonacion"}) {
            Integer existe = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", Integer.class, tabla);
            assertEquals(1, existe, "Falta la tabla " + tabla);
        }
    }

    @Test
    void losDatosOpcionalesDelClienteQuedanVaciosPorDefecto() {
        insertarCliente("99000001");

        String direccion = jdbc.queryForObject("SELECT direccion FROM cliente WHERE cedula = '99000001'", String.class);
        assertEquals("", direccion);
    }

    @Test
    void laBaseDeDatosAsignaIdsCrecientesALosPagos() {
        insertarCliente("99000002");
        long prestamo = insertarPrestamo("99000002");

        jdbc.update("INSERT INTO pago (prestamo_id, fecha, monto, registrado_en) VALUES (?, DATE '2026-12-10', 160000, CURRENT_TIMESTAMP)", prestamo);
        jdbc.update("INSERT INTO pago (prestamo_id, fecha, monto, registrado_en) VALUES (?, DATE '2027-01-10', 660000, CURRENT_TIMESTAMP)", prestamo);

        Long primero = jdbc.queryForObject("SELECT id FROM pago WHERE monto = 160000 AND prestamo_id = ?", Long.class, prestamo);
        Long segundo = jdbc.queryForObject("SELECT id FROM pago WHERE monto = 660000 AND prestamo_id = ?", Long.class, prestamo);
        assertTrue(segundo > primero);
    }

    @Test
    void noSePuedeGuardarUnPrestamoDeUnClienteQueNoExiste() {
        assertThrows(DataIntegrityViolationException.class, () -> insertarPrestamo("99999999"));
    }

    @Test
    void noSePuedeCondonarDosVecesLaMismaCuota() {
        insertarCliente("99000003");
        long prestamo = insertarPrestamo("99000003");
        jdbc.update("INSERT INTO condonacion (prestamo_id, numero_cuota, multa_fijada_en) VALUES (?, 1, 0)", prestamo);

        assertThrows(DataIntegrityViolationException.class, () ->
                jdbc.update("INSERT INTO condonacion (prestamo_id, numero_cuota, multa_fijada_en) VALUES (?, 1, 0)", prestamo));
    }

    @Test
    void losValoresFueraDeLasOpcionesDelContratoSeRechazan() {
        insertarCliente("99000004");

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO prestamo (cedula_cliente, capital, tasa_por_periodo, base_interes, fecha_desembolso,
                    frecuencia, calendario, politica_mora, dias_gracia, orden_imputacion, al_vencer_plazo, interes_cancelacion)
                VALUES ('99000004', 2000000, 8, 'CAPITAL_INICIAL', DATE '2026-11-10', 'DIARIA', 'TODOS_LOS_DIAS',
                    'SIN_MULTA', 2, 'MULTAS_PRIMERO', 'TRATAR_COMO_MORA', 'PERIODO_COMPLETO')
                """));
    }
}
