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
    void hayCuatroClientesDePruebaYTodosEntranConPin1234() {
        for (String cedula : new String[] {"1000000001", "1000000002", "1000000003", "1000000004"}) {
            Cliente cliente = clientes.buscarPorCedula(cedula);
            assertNotNull(cliente, "Falta el cliente " + cedula);
            assertTrue(cifrador.coincide("1234", cliente.getPinCifrado()));
        }
    }

    @Test
    void elPrimerPrestamoDeLauraEstaAlDia() {
        Prestamo prestamo = prestamoDe("1000000001", LocalDate.of(2026, 8, 10));

        assertFalse(tieneCuotasEnMora(prestamo));
        assertEquals(3, prestamo.getPagos().tamano());
        assertEquals(LocalDate.of(2026, 12, 10), prestamo.getCronograma().ultimo().getFechaVencimiento());
    }

    @Test
    void elPrestamoSemanalDeLauraVenceLosMartes() {
        Prestamo prestamo = prestamoDe("1000000001", LocalDate.of(2026, 11, 3));

        assertFalse(tieneCuotasEnMora(prestamo));
        assertEquals(LocalDate.of(2026, 11, 17), prestamo.getCronograma().ultimo().getFechaVencimiento());
    }

    @Test
    void carlosEstaEnMoraConMultas() {
        Prestamo prestamo = prestamoDe("1000000002", LocalDate.of(2026, 7, 5));

        assertTrue(tieneCuotasEnMora(prestamo));
        // Cuota del 5 oct: 40 días de atraso − 2 de gracia = 38 × 2.000 = 76.000.
        Cuota octubre = prestamo.getCronograma().obtener(2);
        assertEquals(LocalDate.of(2026, 10, 5), octubre.getFechaVencimiento());
        assertEquals(0, new BigDecimal("76000").compareTo(prestamo.saldoMulta(octubre, HOY)));
    }

    @Test
    void martaAbonoACapitalYSuCuotaQueCaeDomingoPasaAlLunes() {
        Prestamo prestamo = prestamoDe("1000000003", LocalDate.of(2026, 9, 15));

        assertEquals(0, new BigDecimal("4000000").compareTo(prestamo.getCapitalPendiente()));
        Cuota actual = prestamo.getCronograma().ultimo();
        assertEquals(LocalDate.of(2026, 11, 16), actual.getFechaVencimiento());
        assertEquals(0, new BigDecimal("120000").compareTo(actual.getValor()));
        assertFalse(tieneCuotasEnMora(prestamo));
    }

    @Test
    void elPrestamoDeJorgeEstaCerradoPorCancelacionAnticipada() {
        Prestamo prestamo = prestamoDe("1000000004", LocalDate.of(2026, 6, 20));

        assertTrue(prestamo.estaCerrado());
        assertTrue(prestamo.getPagos().verTope().isCancelacion());
        assertEquals(0, new BigDecimal("1038710").compareTo(prestamo.getPagos().verTope().getMonto()));
    }
}
