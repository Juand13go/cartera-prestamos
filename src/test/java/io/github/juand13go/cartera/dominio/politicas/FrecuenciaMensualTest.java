package io.github.juand13go.cartera.dominio.politicas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class FrecuenciaMensualTest {

    @Test
    void conDiaDeCobro10LasCuotasVencenEl10DeCadaMes() {
        // Caso 1: desembolso el 10 de noviembre de 2026.
        Frecuencia mensual = new FrecuenciaMensual(10);
        LocalDate desembolso = LocalDate.of(2026, 11, 10);

        assertEquals(LocalDate.of(2026, 12, 10), mensual.fechaVencimiento(desembolso, 1));
        assertEquals(LocalDate.of(2027, 1, 10), mensual.fechaVencimiento(desembolso, 2));
        assertEquals(LocalDate.of(2027, 2, 10), mensual.fechaVencimiento(desembolso, 3));
        assertEquals(LocalDate.of(2027, 3, 10), mensual.fechaVencimiento(desembolso, 4));
    }

    @Test
    void conDiaDeCobro30FebreroVenceElUltimoDiaYMarzoVuelveAl30() {
        // Caso 4: cobro el día 30.
        Frecuencia mensual = new FrecuenciaMensual(30);
        LocalDate desembolso = LocalDate.of(2026, 11, 30);

        assertEquals(LocalDate.of(2026, 12, 30), mensual.fechaVencimiento(desembolso, 1));
        assertEquals(LocalDate.of(2027, 1, 30), mensual.fechaVencimiento(desembolso, 2));
        assertEquals(LocalDate.of(2027, 2, 28), mensual.fechaVencimiento(desembolso, 3));
        assertEquals(LocalDate.of(2027, 3, 30), mensual.fechaVencimiento(desembolso, 4));
    }

    @Test
    void conDiaDeCobro31SeUsaElUltimoDiaDeCadaMes() {
        Frecuencia mensual = new FrecuenciaMensual(31);
        LocalDate desembolso = LocalDate.of(2027, 2, 15);

        assertEquals(LocalDate.of(2027, 3, 31), mensual.fechaVencimiento(desembolso, 1));
        assertEquals(LocalDate.of(2027, 4, 30), mensual.fechaVencimiento(desembolso, 2));
        assertEquals(LocalDate.of(2027, 5, 31), mensual.fechaVencimiento(desembolso, 3));
    }

    @Test
    void enAnoBisiestoFebreroVenceEl29() {
        Frecuencia mensual = new FrecuenciaMensual(30);
        LocalDate desembolso = LocalDate.of(2028, 1, 30);

        assertEquals(LocalDate.of(2028, 2, 29), mensual.fechaVencimiento(desembolso, 1));
    }

    @Test
    void laPrimeraCuotaVenceElMesSiguienteAunqueElDiaDeCobroSeaOtro() {
        // Desembolso el 15 de noviembre, cobro el 30: la primera cuota es el 30 de diciembre, no el 30 de noviembre.
        Frecuencia mensual = new FrecuenciaMensual(30);

        assertEquals(LocalDate.of(2026, 12, 30), mensual.fechaVencimiento(LocalDate.of(2026, 11, 15), 1));
    }

    @Test
    void diaDeCobroFueraDeRangoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new FrecuenciaMensual(0));
        assertThrows(IllegalArgumentException.class, () -> new FrecuenciaMensual(32));
    }

    @Test
    void numeroDeCuotaMenorQueUnoLanzaExcepcion() {
        Frecuencia mensual = new FrecuenciaMensual(10);

        assertThrows(IllegalArgumentException.class,
                () -> mensual.fechaVencimiento(LocalDate.of(2026, 11, 10), 0));
    }
}
