package io.github.juand13go.cartera.dominio.politicas;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Cobro una vez por semana, en un día fijo de la semana.
 * La primera cuota vence el primer día de cobro que esté al menos 7 días después del desembolso,
 * para no cobrar una semana completa por pocos días. Las demás, cada 7 días.
 */
public class FrecuenciaSemanal implements Frecuencia {

    private static final int DIAS_MINIMOS_PRIMER_PERIODO = 7;

    private final DayOfWeek diaDeCobro;

    public FrecuenciaSemanal(DayOfWeek diaDeCobro) {
        if (diaDeCobro == null) {
            throw new IllegalArgumentException("Falta el día de cobro");
        }
        this.diaDeCobro = diaDeCobro;
    }

    public DayOfWeek getDiaDeCobro() {
        return diaDeCobro;
    }

    @Override
    public LocalDate fechaVencimiento(LocalDate desembolso, int numeroCuota) {
        if (numeroCuota < 1) {
            throw new IllegalArgumentException("El número de cuota empieza en 1");
        }
        LocalDate primera = desembolso.plusDays(DIAS_MINIMOS_PRIMER_PERIODO)
                .with(TemporalAdjusters.nextOrSame(diaDeCobro));
        return primera.plusWeeks(numeroCuota - 1);
    }
}
