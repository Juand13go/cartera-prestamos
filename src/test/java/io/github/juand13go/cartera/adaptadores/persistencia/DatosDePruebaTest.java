package io.github.juand13go.cartera.adaptadores.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.juand13go.cartera.aplicacion.puertos.CifradorPin;
import io.github.juand13go.cartera.aplicacion.puertos.RepositorioClientes;
import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.Cliente;
import io.github.juand13go.cartera.dominio.modelo.Cuota;
import io.github.juand13go.cartera.dominio.modelo.EstadoCuota;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/**
 * Comprueba que los datos de prueba cargan sin errores y quedan en los estados pensados para la demo.
 * Se revisan a una fecha fija (14 nov 2026) para que la prueba no dependa del día en que se corra.
 */
@SpringBootTest
class DatosDePruebaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 14);

    @Autowired
    private RepositorioClientes clientes;

    @Autowired
    private RepositorioPrestamos prestamos;

    @Autowired
    private CifradorPin cifrador;

    private Prestamo prestamoDe(String cedula, LocalDate desembolso) {
        for (Prestamo prestamo : prestamos.listarPorCliente(cedula)) {
            if (prestamo.getContrato().getFechaDesembolso().equals(desembolso)) {
                prestamo.actualizarCronograma(HOY);
                return prestamo;
            }
        }
        throw new AssertionError("No se encontró el préstamo de " + cedula + " del " + desembolso);
    }

    private static boolean tieneCuotasEnMora(Prestamo prestamo) {
        for (Cuota cuota : prestamo.getCronograma()) {
            if (cuota.estado(HOY) == EstadoCuota.MORA) {
                return true;
            }
        }
        return false;
    }

    @Test
    void hayTresClientesDePruebaYTodosEntranConPin1234() {
        for (String cedula : new String[] {"43586217", "71793468", "1037624519"}) {
            Cliente cliente = clientes.buscarPorCedula(cedula);
            assertNotNull(cliente, "Falta el cliente " + cedula);
            assertTrue(cifrador.coincide("1234", cliente.getPinCifrado()));
        }
    }

    @Test
    void luzMarinaEstaAlDiaYLeTocaPagarElMiercoles18() {
        Prestamo prestamo = prestamoDe("43586217", LocalDate.of(2026, 8, 18));

        assertFalse(tieneCuotasEnMora(prestamo));
        assertEquals(2, prestamo.getPagos().tamano());
        Cuota actual = prestamo.getCronograma().ultimo();
        assertEquals(LocalDate.of(2026, 11, 18), actual.getFechaVencimiento());
        assertEquals(0, new BigDecimal("160000").compareTo(actual.getValor()));
    }

    @Test
    void jhonFredyEstaEnMoraYLeTocaPagarElJueves19() {
        Prestamo prestamo = prestamoDe("71793468", LocalDate.of(2026, 10, 15));

        assertTrue(tieneCuotasEnMora(prestamo));
        // Cuota del 5 nov: 9 días de atraso − 2 de gracia = 7 × 2.000 = 14.000.
        Cuota cinco = prestamo.getCronograma().obtener(2);
        assertEquals(LocalDate.of(2026, 11, 5), cinco.getFechaVencimiento());
        assertEquals(0, new BigDecimal("14000").compareTo(prestamo.saldoMulta(cinco, HOY)));
        assertEquals(LocalDate.of(2026, 11, 19), prestamo.getCronograma().ultimo().getFechaVencimiento());
    }

    @Test
    void gloriaAbonoACapitalYSuCuotaQueCaeDomingoPasaAlLunes16() {
        Prestamo prestamo = prestamoDe("1037624519", LocalDate.of(2026, 9, 30));

        assertEquals(0, new BigDecimal("4000000").compareTo(prestamo.getCapitalPendiente()));
        Cuota actual = prestamo.getCronograma().ultimo();
        assertEquals(LocalDate.of(2026, 11, 16), actual.getFechaVencimiento());
        assertEquals(0, new BigDecimal("120000").compareTo(actual.getValor()));
        assertFalse(tieneCuotasEnMora(prestamo));
    }
}
