package io.github.juand13go.cartera.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.politicas.Calendario;
import io.github.juand13go.cartera.dominio.politicas.Frecuencia;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaMensual;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaSemanal;

class ContratoTest {

    private static final LocalDate DESEMBOLSO = LocalDate.of(2026, 11, 10);

    private static BigDecimal pesos(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void unBorradorSinCambiosTraeLosValoresPorDefecto() {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).firmar();

        assertEquals(pesos("2000000"), contrato.getCapital());
        assertEquals(pesos("8"), contrato.getTasaPorPeriodo());
        assertEquals(DESEMBOLSO, contrato.getFechaDesembolso());
        assertEquals(BaseInteres.CAPITAL_INICIAL, contrato.getBaseInteres());
        assertEquals(Calendario.TODOS_LOS_DIAS, contrato.getCalendario());
    }

    @Test
    void laFrecuenciaPorDefectoEsMensualElDiaDelDesembolso() {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).firmar();

        assertTrue(contrato.getFrecuencia() instanceof FrecuenciaMensual);
        assertEquals(10, ((FrecuenciaMensual) contrato.getFrecuencia()).getDiaDeCobro());
    }

    @Test
    void elBorradorPermiteCambiarLasCondiciones() {
        Frecuencia semanal = new FrecuenciaSemanal(DayOfWeek.FRIDAY);

        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO)
                .base(BaseInteres.CAPITAL_VIGENTE)
                .frecuencia(semanal)
                .calendario(Calendario.SIN_DOMINGOS)
                .firmar();

        assertEquals(BaseInteres.CAPITAL_VIGENTE, contrato.getBaseInteres());
        assertSame(semanal, contrato.getFrecuencia());
        assertEquals(Calendario.SIN_DOMINGOS, contrato.getCalendario());
    }

    @Test
    void capitalOTasaNoPositivosLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(BigDecimal.ZERO, pesos("8"), DESEMBOLSO).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("-1"), DESEMBOLSO).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(null, pesos("8"), DESEMBOLSO).firmar());
    }

    @Test
    void sinFechaBaseOFrecuenciaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("8"), null).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).base(null).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).frecuencia(null).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).calendario(null).firmar());
    }
}
