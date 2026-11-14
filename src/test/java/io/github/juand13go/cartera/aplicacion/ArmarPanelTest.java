package io.github.juand13go.cartera.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.modelo.Cliente;
import io.github.juand13go.cartera.dominio.modelo.Contrato;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;
import io.github.juand13go.cartera.dominio.politicas.MultaFijaDiaria;

/** Panel armado el 8 de diciembre de 2026: la agenda llega hasta el 15 de diciembre. */
class ArmarPanelTest {

    private static final LocalDate HOY = LocalDate.of(2026, 12, 8);

    private RepositorioClientesEnMemoria clientes;
    private RepositorioPrestamosEnMemoria prestamos;

    @BeforeEach
    void preparar() {
        clientes = new RepositorioClientesEnMemoria();
        prestamos = new RepositorioPrestamosEnMemoria();
    }

    private void cliente(String cedula, String nombre) {
        clientes.guardar(new Cliente(cedula, nombre, "3000000000", null, null,
                new BigDecimal("50000000"), null, "cifrado:1234"));
    }

    /** Préstamo mensual de 2.000.000 al 8 % (cuota de 160.000) que cobra el mismo día del desembolso. */
    private long prestamo(String cedula, LocalDate desembolso) {
        return prestamos.crear(cedula, Contrato.borrador(new BigDecimal("2000000"), new BigDecimal("8"), desembolso).firmar());
    }

    private Panel armar() {
        ConsultarPrestamos consultar = new ConsultarPrestamos(prestamos, ConsultarPrestamosTest.relojEn(HOY));
        return new ArmarPanel(consultar, new DirectorioClientes(clientes)).ejecutar();
    }

    @Test
    void laAgendaTraeLosCobrosDeLosProximos7DiasDelMasCercanoAlMasLejano() {
        cliente("1000001", "Ana");
        cliente("1000002", "Beto");
        cliente("1000003", "Ciro");
        long vence10Dic = prestamo("1000001", LocalDate.of(2026, 11, 10));
        long venceHoy = prestamo("1000002", LocalDate.of(2026, 11, 8));
        prestamo("1000003", LocalDate.of(2026, 11, 20)); // vence el 20 dic: fuera de la agenda

        Panel panel = armar();

        assertEquals(HOY, panel.hoy());
        assertEquals(2, panel.agenda().tamano());
        CobroAgendado primero = panel.agenda().extraer();
        assertEquals(venceHoy, primero.prestamoId());
        assertTrue(primero.esHoy());
        assertEquals("Beto", primero.nombreCliente());
        CobroAgendado segundo = panel.agenda().extraer();
        assertEquals(vence10Dic, segundo.prestamoId());
        assertFalse(segundo.esHoy());
        assertEquals(0, new BigDecimal("160000").compareTo(segundo.valorPendiente()));
    }

    @Test
    void laMoraTraePrimeroElPrestamoMasAtrasadoConSuTotalVencido() {
        cliente("1000004", "Dora");
        cliente("1000005", "Elsa");
        // Dora: 1.000.000 al 10 % el día 1, multa de 1.000 diarios. Debe las cuotas del 1 oct, 1 nov y 1 dic.
        long dora = prestamos.crear("1000004", Contrato.borrador(new BigDecimal("1000000"), new BigDecimal("10"),
                LocalDate.of(2026, 9, 1)).mora(new MultaFijaDiaria(new BigDecimal("1000"))).firmar());
        // Elsa: debe la cuota del 25 nov (13 días de atraso).
        long elsa = prestamo("1000005", LocalDate.of(2026, 10, 25));

        Panel panel = armar();

        assertEquals(2, panel.mora().tamano());
        PrestamoEnMora primero = panel.mora().extraer();
        assertEquals(dora, primero.prestamoId());
        assertEquals(68, primero.diasDeAtraso());
        assertEquals(3, primero.cuotasVencidas());
        // 3 cuotas de 100.000 + multas: (66 + 35 + 5) días × 1.000 = 106.000.
        assertEquals(0, new BigDecimal("406000").compareTo(primero.totalVencido()));
        PrestamoEnMora segundo = panel.mora().extraer();
        assertEquals(elsa, segundo.prestamoId());
        assertEquals(13, segundo.diasDeAtraso());
    }

    @Test
    void unPrestamoEnMoraNoEntraALaAgendaSiSuCuotaActualVenceLejos() {
        cliente("1000005", "Elsa");
        // Debe la del 25 nov y su cuota actual vence el 25 dic: fuera de los 7 días, no está en la agenda.
        prestamo("1000005", LocalDate.of(2026, 10, 25));
        cliente("1000006", "Fabio");
        // Debe la del 5 dic y su cuota actual vence el 5 ene: tampoco.
        prestamo("1000006", LocalDate.of(2026, 11, 5));

        Panel panel = armar();

        assertEquals(0, panel.agenda().tamano());
        assertEquals(2, panel.mora().tamano());
    }

    @Test
    void losPrestamosCerradosYLosAlDiaNoAparecenEnLaMora() {
        cliente("1000007", "Gina");
        long cerrado = prestamo("1000007", LocalDate.of(2026, 9, 1));
        Prestamo prestamo = prestamos.buscarPorId(cerrado);
        prestamos.guardarPago(cerrado, prestamo.cancelar(LocalDate.of(2026, 9, 10)), null);
        cliente("1000008", "Hugo");
        prestamo("1000008", LocalDate.of(2026, 12, 1)); // recién desembolsado, al día

        Panel panel = armar();

        assertEquals(0, panel.mora().tamano());
        assertEquals(0, panel.agenda().tamano());
    }

    @Test
    void losTotalesDelResumenSumanCobrosDeHoyMoraYCartera() {
        cliente("1000002", "Beto");
        cliente("1000004", "Dora");
        cliente("1000005", "Elsa");
        cliente("1000007", "Gina");
        prestamo("1000002", LocalDate.of(2026, 11, 8));   // vence hoy: 160.000
        // Dora: 3 cuotas vencidas + multas = 406.000 (ver la prueba de la mora).
        prestamos.crear("1000004", Contrato.borrador(new BigDecimal("1000000"), new BigDecimal("10"),
                LocalDate.of(2026, 9, 1)).mora(new MultaFijaDiaria(new BigDecimal("1000"))).firmar());
        prestamo("1000005", LocalDate.of(2026, 10, 25));  // debe la del 25 nov, sin multa: 160.000
        long cerrado = prestamo("1000007", LocalDate.of(2026, 9, 1));
        prestamos.guardarPago(cerrado, prestamos.buscarPorId(cerrado).cancelar(LocalDate.of(2026, 9, 10)), null);

        Panel panel = armar();

        assertEquals(1, panel.cobrosDeHoy());
        assertEquals(0, new BigDecimal("160000").compareTo(panel.totalCobrosDeHoy()));
        assertEquals(0, new BigDecimal("566000").compareTo(panel.totalEnMora()));
        // El préstamo cerrado no cuenta: 2.000.000 + 1.000.000 + 2.000.000.
        assertEquals(3, panel.prestamosActivos());
        assertEquals(0, new BigDecimal("5000000").compareTo(panel.carteraTotal()));
    }

    @Test
    void recorrerLasColasConForEachNoLasVacia() {
        cliente("1000001", "Ana");
        cliente("1000002", "Beto");
        prestamo("1000001", LocalDate.of(2026, 11, 10));
        prestamo("1000002", LocalDate.of(2026, 11, 8));
        Panel panel = armar();

        int vistos = 0;
        for (CobroAgendado cobro : panel.agenda()) {
            vistos++;
        }

        assertEquals(2, vistos);
        assertEquals(2, panel.agenda().tamano());
    }
}
