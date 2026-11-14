package io.github.juand13go.cartera.dominio.politicas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class FrecuenciaSemanalTest {

    /** El 10 de noviembre de 2026 es martes. */
    private static final LocalDate DESEMBOLSO_MARTES = LocalDate.of(2026, 11, 10);

    @Test
    void elDesembolsoDePruebaEsMartes() {
        assertEquals(DayOfWeek.TUESDAY, DESEMBOLSO_MARTES.getDayOfWeek());
    }

    @Test
    void conCobroElMismoDiaDelDesembolsoLasCuotasVencenCada7Dias() {
        Frecuencia semanal = new FrecuenciaSemanal(DayOfWeek.TUESDAY);

        assertEquals(LocalDate.of(2026, 11, 17), semanal.fechaVencimiento(DESEMBOLSO_MARTES, 1));
        assertEquals(LocalDate.of(2026, 11, 24), semanal.fechaVencimiento(DESEMBOLSO_MARTES, 2));
        assertEquals(LocalDate.of(2026, 12, 1), semanal.fechaVencimiento(DESEMBOLSO_MARTES, 3));
    }

    @Test
    void laPrimeraCuotaEsElPrimerDiaDeCobroAlMenos7DiasDespues() {
        // Martes + 7 días = martes 17; el viernes siguiente es el 20 (10 días).
        assertEquals(LocalDate.of(2026, 11, 20),
                new FrecuenciaSemanal(DayOfWeek.FRIDAY).fechaVencimiento(DESEMBOLSO_MARTES, 1));
        // El lunes siguiente al martes 17 es el 23 (13 días), nunca el lunes 16 (6 días).
        assertEquals(LocalDate.of(2026, 11, 23),
                new FrecuenciaSemanal(DayOfWeek.MONDAY).fechaVencimiento(DESEMBOLSO_MARTES, 1));
    }

    @Test
    void cruzaElCambioDeAnoSinProblema() {
        Frecuencia semanal = new FrecuenciaSemanal(DayOfWeek.TUESDAY);

        assertEquals(LocalDate.of(2026, 12, 29), semanal.fechaVencimiento(DESEMBOLSO_MARTES, 7));
        assertEquals(LocalDate.of(2027, 1, 5), semanal.fechaVencimiento(DESEMBOLSO_MARTES, 8));
    }

    @Test
    void sinDiaDeCobroONumeroInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new FrecuenciaSemanal(null));
        assertThrows(IllegalArgumentException.class,
                () -> new FrecuenciaSemanal(DayOfWeek.TUESDAY).fechaVencimiento(DESEMBOLSO_MARTES, 0));
    }

    @Test
    void elPlazoMaximoSon104CuotasSemanales() {
        assertEquals(104, new FrecuenciaSemanal(DayOfWeek.TUESDAY).cuotasEnPlazoMaximo());
    }
}
