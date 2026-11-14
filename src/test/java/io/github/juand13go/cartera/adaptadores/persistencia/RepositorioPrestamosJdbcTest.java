package io.github.juand13go.cartera.adaptadores.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import io.github.juand13go.cartera.aplicacion.puertos.RegistroDePago;
import io.github.juand13go.cartera.aplicacion.puertos.RepositorioClientes;
import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.BaseInteres;
import io.github.juand13go.cartera.dominio.modelo.Cliente;
import io.github.juand13go.cartera.dominio.modelo.Contrato;
import io.github.juand13go.cartera.dominio.modelo.Cuota;
import io.github.juand13go.cartera.dominio.modelo.Pago;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;
import io.github.juand13go.cartera.dominio.politicas.AlVencerPlazo;
import io.github.juand13go.cartera.dominio.politicas.Calendario;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaMensual;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaQuincenal;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaSemanal;
import io.github.juand13go.cartera.dominio.politicas.InteresCancelacion;
import io.github.juand13go.cartera.dominio.politicas.MultaFijaDiaria;
import io.github.juand13go.cartera.dominio.politicas.OrdenImputacion;
import io.github.juand13go.cartera.dominio.politicas.PorcentajeDiario;
import io.github.juand13go.cartera.dominio.politicas.PorcentajeUnico;
import io.github.juand13go.cartera.dominio.politicas.SinMulta;

@SpringBootTest
@Transactional
class RepositorioPrestamosJdbcTest {

    private static final String CEDULA = "77000001";
    private static final LocalDate DESEMBOLSO = LocalDate.of(2026, 11, 10);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 14, 10, 30);

    @Autowired
    private RepositorioPrestamos repositorio;

    @Autowired
    private RepositorioClientes clientes;

    @BeforeEach
    void crearCliente() {
        clientes.guardar(new Cliente(CEDULA, "Cliente de prueba", "3001234567", null, null,
                new BigDecimal("10000000"), null, "$2a$10$hashDePrueba"));
    }

    private static BigDecimal pesos(String valor) {
        return new BigDecimal(valor);
    }

    private static boolean iguales(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) == 0;
    }

    /** Hace en el dominio lo mismo que haría un caso de uso: ejecuta la acción y guarda solo lo nuevo. */
    private void pagar(Prestamo prestamo, LocalDate fecha, String monto) {
        Pago pago = prestamo.registrarPago(fecha, pesos(monto));
        repositorio.guardarPago(prestamo.getId(), pago, AHORA);
    }

    // Contrato

    @Test
    void guardaYLeeTodasLasCondicionesDelContrato() {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8.5"), DESEMBOLSO)
                .base(BaseInteres.CAPITAL_VIGENTE)
                .frecuencia(new FrecuenciaSemanal(DayOfWeek.FRIDAY))
                .calendario(Calendario.SIN_DOMINGOS)
                .mora(new PorcentajeDiario(pesos("1.5")))
                .diasDeGracia(3)
                .ordenImputacion(OrdenImputacion.CUOTAS_PRIMERO)
                .alVencerPlazo(AlVencerPlazo.SEGUIR_COBRANDO)
                .interesCancelacion(InteresCancelacion.POR_DIAS)
                .firmar();

        long id = repositorio.crear(CEDULA, contrato);
        Prestamo leido = repositorio.buscarPorId(id);

        assertNotNull(leido);
        assertEquals(id, leido.getId());
        assertEquals(CEDULA, leido.getCedulaCliente());
        Contrato c = leido.getContrato();
        assertTrue(iguales(pesos("2000000"), c.getCapital()));
        assertTrue(iguales(pesos("8.5"), c.getTasaPorPeriodo()));
        assertEquals(DESEMBOLSO, c.getFechaDesembolso());
        assertEquals(BaseInteres.CAPITAL_VIGENTE, c.getBaseInteres());
        assertEquals(DayOfWeek.FRIDAY, ((FrecuenciaSemanal) c.getFrecuencia()).getDiaDeCobro());
        assertEquals(Calendario.SIN_DOMINGOS, c.getCalendario());
        assertTrue(iguales(pesos("1.5"), ((PorcentajeDiario) c.getPoliticaMora()).getPorcentaje()));
        assertEquals(3, c.getDiasDeGracia());
        assertEquals(OrdenImputacion.CUOTAS_PRIMERO, c.getOrdenImputacion());
        assertEquals(AlVencerPlazo.SEGUIR_COBRANDO, c.getAlVencerPlazo());
        assertEquals(InteresCancelacion.POR_DIAS, c.getInteresCancelacion());
    }

    @Test
    void guardaYLeeLasDemasFrecuenciasYPoliticasDeMora() {
        long quincenalConMultaFija = repositorio.crear(CEDULA, Contrato.borrador(pesos("1000000"), pesos("5"), DESEMBOLSO)
                .frecuencia(new FrecuenciaQuincenal())
                .mora(new MultaFijaDiaria(pesos("2000")))
                .firmar());
        long mensualConPorcentajeUnico = repositorio.crear(CEDULA, Contrato.borrador(pesos("1000000"), pesos("5"), DESEMBOLSO)
                .frecuencia(new FrecuenciaMensual(30))
                .mora(new PorcentajeUnico(pesos("10")))
                .firmar());
        long sinMulta = repositorio.crear(CEDULA, Contrato.borrador(pesos("1000000"), pesos("5"), DESEMBOLSO).firmar());

        Contrato primero = repositorio.buscarPorId(quincenalConMultaFija).getContrato();
        assertTrue(primero.getFrecuencia() instanceof FrecuenciaQuincenal);
        assertEquals(pesos("2000"), ((MultaFijaDiaria) primero.getPoliticaMora()).getValorDiario());

        Contrato segundo = repositorio.buscarPorId(mensualConPorcentajeUnico).getContrato();
        assertEquals(30, ((FrecuenciaMensual) segundo.getFrecuencia()).getDiaDeCobro());
        assertTrue(iguales(pesos("10"), ((PorcentajeUnico) segundo.getPoliticaMora()).getPorcentaje()));

        assertTrue(repositorio.buscarPorId(sinMulta).getContrato().getPoliticaMora() instanceof SinMulta);
    }

    @Test
    void buscarUnPrestamoQueNoExisteDevuelveNull() {
        assertNull(repositorio.buscarPorId(999999));
    }

    // Reconstrucción

    @Test
    void alLeerloSeReconstruyeConSusPagosYCondonaciones() {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO)
                .mora(new MultaFijaDiaria(pesos("2000")))
                .firmar();
        Prestamo prestamo = new Prestamo(repositorio.crear(CEDULA, contrato), CEDULA, contrato);
        pagar(prestamo, LocalDate.of(2026, 12, 15), "4000");
        BigDecimal fijadaEn = prestamo.condonarMulta(1, LocalDate.of(2026, 12, 15));
        repositorio.guardarCondonacion(prestamo.getId(), 1, fijadaEn);
        pagar(prestamo, LocalDate.of(2026, 12, 20), "170000");

        Prestamo leido = repositorio.buscarPorId(prestamo.getId());

        assertEquals(2, leido.getPagos().tamano());
        Cuota primera = leido.getCronograma().primero();
        assertTrue(primera.estaMultaCondonada());
        assertTrue(iguales(pesos("4000"), primera.getMultaPagada()));
        assertTrue(primera.estaCubierta());
        assertTrue(iguales(prestamo.getCapitalPendiente(), leido.getCapitalPendiente()));
        Pago ultimo = leido.getPagos().verTope();
        assertTrue(iguales(pesos("0"), ultimo.getAbonoAMultas()));
        assertTrue(iguales(pesos("170000"), ultimo.getAbonoACuotas()));
    }

    @Test
    void unaCancelacionGuardadaSeRepiteComoCancelacion() {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO)
                .interesCancelacion(InteresCancelacion.POR_DIAS)
                .firmar();
        Prestamo prestamo = new Prestamo(repositorio.crear(CEDULA, contrato), CEDULA, contrato);
        repositorio.guardarPago(prestamo.getId(), prestamo.cancelar(LocalDate.of(2026, 11, 25)), AHORA);

        Prestamo leido = repositorio.buscarPorId(prestamo.getId());

        assertTrue(leido.estaCerrado());
        assertTrue(leido.getPagos().verTope().isCancelacion());
        assertTrue(iguales(pesos("80000"), leido.getCronograma().primero().getValor()));
    }

    @Test
    void borrarElUltimoPagoDejaSoloLosAnteriores() {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).firmar();
        Prestamo prestamo = new Prestamo(repositorio.crear(CEDULA, contrato), CEDULA, contrato);
        pagar(prestamo, LocalDate.of(2026, 12, 10), "160000");
        pagar(prestamo, LocalDate.of(2027, 1, 10), "660000");

        repositorio.borrarUltimoPago(prestamo.getId());

        Prestamo leido = repositorio.buscarPorId(prestamo.getId());
        assertEquals(1, leido.getPagos().tamano());
        assertTrue(iguales(pesos("2000000"), leido.getCapitalPendiente()));
    }

    @Test
    void losRegistrosDePagoVienenEnOrdenConSuIdYFechaHora() {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).firmar();
        Prestamo prestamo = new Prestamo(repositorio.crear(CEDULA, contrato), CEDULA, contrato);
        repositorio.guardarPago(prestamo.getId(), prestamo.registrarPago(LocalDate.of(2026, 12, 10), pesos("160000")),
                LocalDateTime.of(2026, 12, 10, 9, 15));
        repositorio.guardarPago(prestamo.getId(), prestamo.registrarPago(LocalDate.of(2027, 1, 10), pesos("660000")),
                LocalDateTime.of(2027, 1, 10, 16, 40));

        RegistroDePago primero = repositorio.registrosDePago(prestamo.getId()).primero();
        RegistroDePago segundo = repositorio.registrosDePago(prestamo.getId()).ultimo();

        assertEquals(2, repositorio.registrosDePago(prestamo.getId()).tamano());
        assertTrue(segundo.id() > primero.id());
        assertEquals(LocalDateTime.of(2026, 12, 10, 9, 15), primero.registradoEn());
        assertEquals(LocalDateTime.of(2027, 1, 10, 16, 40), segundo.registradoEn());
    }

    // Listados

    @Test
    void listarPorClienteTraeSoloLosDeEseClienteDelMasRecienteAlMasAntiguo() {
        clientes.guardar(new Cliente("77000002", "Otro cliente", "3007654321", null, null,
                new BigDecimal("10000000"), null, "$2a$10$hashDePrueba"));
        Contrato contrato = Contrato.borrador(pesos("1000000"), pesos("5"), DESEMBOLSO).firmar();
        long viejo = repositorio.crear(CEDULA, contrato);
        long nuevo = repositorio.crear(CEDULA, contrato);
        repositorio.crear("77000002", contrato);

        int cantidad = 0;
        long anterior = Long.MAX_VALUE;
        for (Prestamo p : repositorio.listarPorCliente(CEDULA)) {
            assertEquals(CEDULA, p.getCedulaCliente());
            assertTrue(p.getId() < anterior);
            anterior = p.getId();
            cantidad++;
        }
        assertEquals(2, cantidad);
        assertEquals(nuevo, repositorio.listarPorCliente(CEDULA).primero().getId());
        assertEquals(viejo, repositorio.listarPorCliente(CEDULA).ultimo().getId());
    }

    @Test
    void listarTodosIncluyePrestamosDeTodosLosClientes() {
        Contrato contrato = Contrato.borrador(pesos("1000000"), pesos("5"), DESEMBOLSO).firmar();
        long id = repositorio.crear(CEDULA, contrato);

        boolean encontrado = false;
        for (Prestamo p : repositorio.listarTodos()) {
            if (p.getId() == id) {
                encontrado = true;
            }
        }
        assertTrue(encontrado);
        assertFalse(repositorio.listarTodos().estaVacia());
    }
}
