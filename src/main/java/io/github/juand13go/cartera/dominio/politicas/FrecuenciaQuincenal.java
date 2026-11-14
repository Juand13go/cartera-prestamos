package io.github.juand13go.cartera.dominio.politicas;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Cobro dos veces al mes: el día 15 y el último día del mes.
 * La primera cuota vence en el primer corte que esté al menos 15 días después del desembolso,
 * para no cobrar una quincena completa por pocos días.
 *
 * Para contar cortes sin recorrer fechas, cada corte tiene un número:
 * cada mes aporta dos cortes, el 0 (día 15) y el 1 (último día).
 * Así, avanzar N cuotas es sumar N a ese número.
 */
public class FrecuenciaQuincenal implements Frecuencia {

    private static final int DIAS_MINIMOS_PRIMER_PERIODO = 15;

    @Override
    public LocalDate fechaVencimiento(LocalDate desembolso, int numeroCuota) {
        if (numeroCuota < 1) {
            throw new IllegalArgumentException("El número de cuota empieza en 1");
        }
        int corte = primerCorte(desembolso) + (numeroCuota - 1);
        return fechaDelCorte(corte);
    }

    /** Número del primer corte que cae al menos 15 días después del desembolso. */
    private int primerCorte(LocalDate desembolso) {
        LocalDate minimo = desembolso.plusDays(DIAS_MINIMOS_PRIMER_PERIODO);
        int corteDelMes = minimo.getDayOfMonth() <= 15 ? 0 : 1;
        return numeroDelMes(YearMonth.from(minimo)) * 2 + corteDelMes;
    }

    /** Convierte el número de corte en su fecha: día 15 o último día del mes. */
    private LocalDate fechaDelCorte(int corte) {
        int numeroMes = corte / 2;
        YearMonth mes = YearMonth.of(numeroMes / 12, numeroMes % 12 + 1);
        if (corte % 2 == 0) {
            return mes.atDay(15);
        }
        return mes.atEndOfMonth();
    }

    /** Número consecutivo del mes: enero del año 0 es el 0, febrero el 1, y así. */
    private int numeroDelMes(YearMonth mes) {
        return mes.getYear() * 12 + (mes.getMonthValue() - 1);
    }
}
