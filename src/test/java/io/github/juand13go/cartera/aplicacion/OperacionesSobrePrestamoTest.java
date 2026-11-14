package io.github.juand13go.cartera.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.modelo.Contrato;
import io.github.juand13go.cartera.dominio.modelo.Pago;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;
import io.github.juand13go.cartera.dominio.politicas.InteresCancelacion;
import io.github.juand13go.cartera.dominio.politicas.MultaFijaDiaria;

/** Pagar, deshacer, condonar y cancelar: cada caso de uso guarda solo lo nuevo, y al releer se ve el cambio. */
class OperacionesSobrePrestamoTest {

    private static final LocalDate DESEMBOLSO = LocalDate.of(2026, 11, 10);

    private final RepositorioPrestamosEnMemoria repositorio = new RepositorioPrestamosEnMemoria();

    private long crearCaso1() {
        return repositorio.crear("1001001", Contrato.borrador(new BigDecimal("2000000"), new BigDecimal("8"), DESEMBOLSO)
                .mora(new MultaFijaDiaria(new BigDecimal("2000")))
                .interesCancelacion(InteresCancelacion.POR_DIAS)
                .firmar());
    }

    private static Clock relojEn(LocalDate fecha) {
        return ConsultarPrestamosTest.relojEn(fecha);
    }

    // Registrar pago

    @Test
    void registrarPagoUsaLaFechaDeHoyYGuardaElPago() {
        long id = crearCaso1();
        RegistrarPago registrar = new RegistrarPago(repositorio, relojEn(LocalDate.of(2026, 12, 10)));

        Pago pago = registrar.ejecutar(id, new BigDecimal("160000"));

        assertEquals(LocalDate.of(2026, 12, 10), pago.getFecha());
        assertEquals(LocalDateTime.of(2026, 12, 10, 12, 0), repositorio.ultimoRegistradoEn);
        Prestamo releido = repositorio.buscarPorId(id);
        assertEquals(1, releido.getPagos().tamano());
        assertTrue(releido.getCronograma().primero().estaCubierta());
    }

    @Test
    void siElDominioRechazaElPagoNoSeGuardaNada() {
        long id = crearCaso1();
        RegistrarPago registrar = new RegistrarPago(repositorio, relojEn(LocalDate.of(2026, 12, 10)));

        assertThrows(IllegalArgumentException.class, () -> registrar.ejecutar(id, new BigDecimal("9999999")));
        assertEquals(0, repositorio.pagosGuardados);
    }

    @Test
    void unPagoConCentavosOAUnPrestamoInexistenteSeRechaza() {
        long id = crearCaso1();
        RegistrarPago registrar = new RegistrarPago(repositorio, relojEn(LocalDate.of(2026, 12, 10)));

        assertThrows(IllegalArgumentException.class, () -> registrar.ejecutar(id, new BigDecimal("1000.50")));
        assertThrows(IllegalArgumentException.class, () -> registrar.ejecutar(99, new BigDecimal("1000")));
    }

    // Deshacer

    @Test
    void deshacerBorraElUltimoPago() {
        long id = crearCaso1();
        new RegistrarPago(repositorio, relojEn(LocalDate.of(2026, 12, 10))).ejecutar(id, new BigDecimal("160000"));
        new RegistrarPago(repositorio, relojEn(LocalDate.of(2027, 1, 10))).ejecutar(id, new BigDecimal("660000"));

        Pago deshecho = new DeshacerUltimoPago(repositorio).ejecutar(id);

        assertEquals(0, new BigDecimal("660000").compareTo(deshecho.getMonto()));
        Prestamo releido = repositorio.buscarPorId(id);
        assertEquals(1, releido.getPagos().tamano());
        assertEquals(0, new BigDecimal("2000000").compareTo(releido.getCapitalPendiente()));
    }

    @Test
    void deshacerSinPagosLanzaExcepcion() {
        long id = crearCaso1();

        assertThrows(IllegalStateException.class, () -> new DeshacerUltimoPago(repositorio).ejecutar(id));
    }

    // Condonar

    @Test
    void condonarGuardaLaCondonacionYSeRespetaAlReleer() {
        long id = crearCaso1();
        LocalDate quinceDic = LocalDate.of(2026, 12, 15);

        BigDecimal fijadaEn = new CondonarMulta(repositorio, relojEn(quinceDic)).ejecutar(id, 1);

        assertEquals(0, BigDecimal.ZERO.compareTo(fijadaEn));
        Prestamo releido = repositorio.buscarPorId(id);
        releido.actualizarCronograma(quinceDic);
        assertTrue(releido.getCronograma().primero().estaMultaCondonada());
        assertEquals(0, BigDecimal.ZERO.compareTo(releido.saldoMulta(releido.getCronograma().primero(), quinceDic)));
    }

    // Cancelar

    @Test
    void cancelarCierraElPrestamoYQuedaGuardado() {
        long id = crearCaso1();

        Pago pago = new CancelarPrestamo(repositorio, relojEn(LocalDate.of(2026, 11, 25))).ejecutar(id);

        assertTrue(pago.isCancelacion());
        assertEquals(0, new BigDecimal("2080000").compareTo(pago.getMonto()));
        assertTrue(repositorio.buscarPorId(id).estaCerrado());
    }

    @Test
    void despuesDeDeshacerLaCancelacionElPrestamoQuedaAbierto() {
        long id = crearCaso1();
        new CancelarPrestamo(repositorio, relojEn(LocalDate.of(2026, 11, 25))).ejecutar(id);

        new DeshacerUltimoPago(repositorio).ejecutar(id);

        assertFalse(repositorio.buscarPorId(id).estaCerrado());
    }
}
