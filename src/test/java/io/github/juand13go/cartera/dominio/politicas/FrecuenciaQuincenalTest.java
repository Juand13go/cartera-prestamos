package io.github.juand13go.cartera.dominio.politicas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class FrecuenciaQuincenalTest {

    private final Frecuencia quincenal = new FrecuenciaQuincenal();

    @Test
    void alternaEntreElDia15YElUltimoDiaDeCadaMes() {
        LocalDate desembolso = LocalDate.of(2026, 11, 15);

        assertEquals(LocalDate.of(2026, 11, 30), quincenal.fechaVencimiento(desembolso, 1));
        assertEquals(LocalDate.of(2026, 12, 15), quincenal.fechaVencimiento(desembolso, 2));
        assertEquals(LocalDate.of(2026, 12, 31), quincenal.fechaVencimiento(desembolso, 3));
        assertEquals(LocalDate.of(2027, 1, 15), quincenal.fechaVencimiento(desembolso, 4));
        assertEquals(LocalDate.of(2027, 1, 31), quincenal.fechaVencimiento(desembolso, 5));
    }

    @Test
    void enFebreroElSegundoCorteEsEl28() {
        LocalDate desembolso = LocalDate.of(2026, 11, 15);

        assertEquals(LocalDate.of(2027, 2, 15), quincenal.fechaVencimiento(desembolso, 6));
        assertEquals(LocalDate.of(2027, 2, 28), quincenal.fechaVencimiento(desembolso, 7));
        assertEquals(LocalDate.of(2027, 3, 15), quincenal.fechaVencimiento(desembolso, 8));
    }

    @Test
    void laPrimeraCuotaEstaAlMenos15DiasDespuesDelDesembolso() {
        // Desembolso el 1 nov: el 15 nov está a solo 14 días, así que la primera es el 30 nov.
        assertEquals(LocalDate.of(2026, 11, 30), quincenal.fechaVencimiento(LocalDate.of(2026, 11, 1), 1));
        // Desembolso el 30 nov: el 15 dic está justo a 15 días.
        assertEquals(LocalDate.of(2026, 12, 15), quincenal.fechaVencimiento(LocalDate.of(2026, 11, 30), 1));
        // Desembolso el 31 ene: el 15 feb está a 15 días.
        assertEquals(LocalDate.of(2027, 2, 15), quincenal.fechaVencimiento(LocalDate.of(2027, 1, 31), 1));
    }

    @Test
    void numeroDeCuotaMenorQueUnoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> quincenal.fechaVencimiento(LocalDate.of(2026, 11, 15), 0));
    }
}
