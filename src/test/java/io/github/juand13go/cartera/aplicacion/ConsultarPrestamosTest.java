package io.github.juand13go.cartera.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.modelo.Contrato;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

class ConsultarPrestamosTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate DESEMBOLSO = LocalDate.of(2026, 11, 10);

    /** Un reloj detenido en esa fecha, a mediodía en Bogotá. */
    static Clock relojEn(LocalDate fecha) {
        return Clock.fixed(fecha.atTime(12, 0).atZone(BOGOTA).toInstant(), BOGOTA);
    }

    private static Contrato contrato() {
        return Contrato.borrador(new BigDecimal("2000000"), new BigDecimal("8"), DESEMBOLSO).firmar();
    }

    @Test
    void hoyEsLaFechaDelReloj() {
        ConsultarPrestamos consultar = new ConsultarPrestamos(new RepositorioPrestamosEnMemoria(), relojEn(DESEMBOLSO));

        assertEquals(DESEMBOLSO, consultar.hoy());
    }

    @Test
    void elPrestamoSeMuestraConLasCuotasQueYaNacieronHoy() {
        RepositorioPrestamosEnMemoria repositorio = new RepositorioPrestamosEnMemoria();
        long id = repositorio.crear("1001001", contrato());
        ConsultarPrestamos consultar = new ConsultarPrestamos(repositorio, relojEn(LocalDate.of(2027, 1, 11)));

        Prestamo prestamo = consultar.porId(id);

        // 10 dic, 10 ene y la del 10 feb, que nació el 11 ene.
        assertEquals(3, prestamo.getCronograma().tamano());
    }

    @Test
    void unPrestamoQueNoExisteLanzaExcepcion() {
        ConsultarPrestamos consultar = new ConsultarPrestamos(new RepositorioPrestamosEnMemoria(), relojEn(DESEMBOLSO));

        assertThrows(IllegalArgumentException.class, () -> consultar.porId(99));
    }

    @Test
    void losListadosTambienSeActualizanAHoy() {
        RepositorioPrestamosEnMemoria repositorio = new RepositorioPrestamosEnMemoria();
        repositorio.crear("1001001", contrato());
        repositorio.crear("1001002", contrato());
        ConsultarPrestamos consultar = new ConsultarPrestamos(repositorio, relojEn(LocalDate.of(2026, 12, 11)));

        assertEquals(1, consultar.deCliente("1001001").tamano());
        assertEquals(2, consultar.deCliente("1001001").primero().getCronograma().tamano());
        for (Prestamo prestamo : consultar.todos()) {
            assertEquals(2, prestamo.getCronograma().tamano());
        }
    }
}
