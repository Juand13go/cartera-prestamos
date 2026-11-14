package io.github.juand13go.cartera.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.modelo.BaseInteres;
import io.github.juand13go.cartera.dominio.modelo.Contrato;

/** Caso 2 simulado: ya pagó la cuota del 10 dic y el 10 ene pregunta "¿y si abono 660.000?". */
class SimularAbonoTest {

    private static final LocalDate DIEZ_ENE = LocalDate.of(2027, 1, 10);

    private final RepositorioPrestamosEnMemoria repositorio = new RepositorioPrestamosEnMemoria();

    private long caso2(BaseInteres base) {
        long id = repositorio.crear("1000000001", Contrato.borrador(new BigDecimal("2000000"), new BigDecimal("8"),
                LocalDate.of(2026, 11, 10)).base(base).firmar());
        new RegistrarPago(repositorio, ConsultarPrestamosTest.relojEn(LocalDate.of(2026, 12, 10)))
                .ejecutar(id, new BigDecimal("160000"));
        return id;
    }

    private SimularAbono simulador() {
        return new SimularAbono(repositorio, ConsultarPrestamosTest.relojEn(DIEZ_ENE));
    }

    @Test
    void conCapitalVigenteElAbonoBajaElCapitalYElInteres() {
        long id = caso2(BaseInteres.CAPITAL_VIGENTE);

        ResultadoSimulacion resultado = simulador().ejecutar(id, new BigDecimal("660000"));

        assertTrue(resultado.baseVigente());
        assertEquals(0, new BigDecimal("2000000").compareTo(resultado.capitalAntes()));
        assertEquals(0, new BigDecimal("1500000").compareTo(resultado.capitalDespues()));
        assertEquals(0, new BigDecimal("160000").compareTo(resultado.interesAntes()));
        assertEquals(0, new BigDecimal("120000").compareTo(resultado.interesDespues()));
        assertEquals(0, new BigDecimal("500000").compareTo(resultado.pago().getAbonoACapital()));
    }

    @Test
    void conCapitalInicialElInteresNoCambia() {
        long id = caso2(BaseInteres.CAPITAL_INICIAL);

        ResultadoSimulacion resultado = simulador().ejecutar(id, new BigDecimal("660000"));

        assertFalse(resultado.baseVigente());
        assertEquals(0, new BigDecimal("1500000").compareTo(resultado.capitalDespues()));
        assertEquals(0, new BigDecimal("160000").compareTo(resultado.interesDespues()));
    }

    @Test
    void simularNoGuardaNada() {
        long id = caso2(BaseInteres.CAPITAL_VIGENTE);
        int guardadosAntes = repositorio.pagosGuardados;

        simulador().ejecutar(id, new BigDecimal("660000"));

        assertEquals(guardadosAntes, repositorio.pagosGuardados);
        assertEquals(1, repositorio.buscarPorId(id).getPagos().tamano());
        assertEquals(0, new BigDecimal("2000000").compareTo(repositorio.buscarPorId(id).capitalAdeudado()));
    }

    @Test
    void unAbonoImposibleSeRechazaConElMensajeDelDominio() {
        long id = caso2(BaseInteres.CAPITAL_VIGENTE);

        assertThrows(IllegalArgumentException.class, () -> simulador().ejecutar(id, new BigDecimal("99999999")));
        assertThrows(IllegalArgumentException.class, () -> simulador().ejecutar(id, new BigDecimal("1000.50")));
        assertThrows(IllegalArgumentException.class, () -> simulador().ejecutar(999, new BigDecimal("1000")));
    }
}
