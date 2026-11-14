package io.github.juand13go.cartera.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.modelo.Cliente;
import io.github.juand13go.cartera.dominio.modelo.Contrato;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

class CrearPrestamoTest {

    private static final String CEDULA = "1020304050";
    private static final LocalDate DESEMBOLSO = LocalDate.of(2026, 11, 10);

    private RepositorioPrestamosEnMemoria repositorio;
    private CrearPrestamo crearPrestamo;

    @BeforeEach
    void preparar() {
        RepositorioClientesEnMemoria clientes = new RepositorioClientesEnMemoria();
        // Cupo de 5.000.000
        clientes.guardar(new Cliente(CEDULA, "Ana Pérez", "3001234567", null, null,
                new BigDecimal("5000000"), null, "cifrado:1234"));
        repositorio = new RepositorioPrestamosEnMemoria();
        crearPrestamo = new CrearPrestamo(repositorio, new DirectorioClientes(clientes), new BigDecimal("50000"));
    }

    private static Contrato contrato(String capital) {
        return Contrato.borrador(new BigDecimal(capital), new BigDecimal("8"), DESEMBOLSO).firmar();
    }

    @Test
    void creaElPrestamoConElIdQueAsignaElRepositorio() {
        Prestamo prestamo = crearPrestamo.ejecutar(CEDULA, contrato("2000000"), false);

        assertTrue(prestamo.getId() > 0);
        assertEquals(CEDULA, prestamo.getCedulaCliente());
        assertNotNull(repositorio.buscarPorId(prestamo.getId()));
    }

    @Test
    void elClienteDebeExistir() {
        assertThrows(IllegalArgumentException.class, () -> crearPrestamo.ejecutar("9999999", contrato("2000000"), false));
    }

    @Test
    void elCapitalDebeSerAlMenosElMontoMinimo() {
        assertThrows(IllegalArgumentException.class, () -> crearPrestamo.ejecutar(CEDULA, contrato("49999"), false));
        assertNotNull(crearPrestamo.ejecutar(CEDULA, contrato("50000"), false));
    }

    @Test
    void elCapitalNoPuedeTenerCentavos() {
        assertThrows(IllegalArgumentException.class, () -> crearPrestamo.ejecutar(CEDULA, contrato("100000.50"), false));
    }

    @Test
    void siSuperaElCupoPideConfirmacionConLosValores() {
        crearPrestamo.ejecutar(CEDULA, contrato("4000000"), false);

        CupoExcedidoException excepcion = assertThrows(CupoExcedidoException.class,
                () -> crearPrestamo.ejecutar(CEDULA, contrato("2000000"), false));

        assertEquals(0, new BigDecimal("5000000").compareTo(excepcion.getCupo()));
        assertEquals(0, new BigDecimal("4000000").compareTo(excepcion.getDeudaVigente()));
        assertEquals(0, new BigDecimal("2000000").compareTo(excepcion.getMontoSolicitado()));
        assertEquals(1, repositorio.listarPorCliente(CEDULA).tamano());
    }

    @Test
    void conConfirmacionSeCreaAunqueSupereElCupo() {
        crearPrestamo.ejecutar(CEDULA, contrato("4000000"), false);

        Prestamo segundo = crearPrestamo.ejecutar(CEDULA, contrato("2000000"), true);

        assertNotNull(segundo);
        assertEquals(2, repositorio.listarPorCliente(CEDULA).tamano());
    }

    @Test
    void justoEnElCupoNoPideConfirmacion() {
        crearPrestamo.ejecutar(CEDULA, contrato("3000000"), false);

        assertNotNull(crearPrestamo.ejecutar(CEDULA, contrato("2000000"), false));
    }

    @Test
    void losPrestamosCerradosNoCuentanEnLaDeuda() {
        Prestamo viejo = crearPrestamo.ejecutar(CEDULA, contrato("4000000"), false);
        repositorio.guardarPago(viejo.getId(), viejo.cancelar(DESEMBOLSO.plusDays(5)), null);

        assertNotNull(crearPrestamo.ejecutar(CEDULA, contrato("4000000"), false));
    }

    @Test
    void laDeudaEsElCapitalQueFaltaYBajaConLosAbonos() {
        Prestamo primero = crearPrestamo.ejecutar(CEDULA, contrato("4000000"), false);
        // Paga la cuota (320.000) y abona 1.000.000 a capital: debe 3.000.000.
        repositorio.guardarPago(primero.getId(),
                primero.registrarPago(LocalDate.of(2026, 12, 10), new BigDecimal("1320000")), null);

        assertNotNull(crearPrestamo.ejecutar(CEDULA, contrato("2000000"), false));
    }
}
