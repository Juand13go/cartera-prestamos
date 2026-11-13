package io.github.juand13go.cartera.dominio.politicas;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Cobro una vez al mes, en un día fijo. Si el mes no tiene ese día (por ejemplo, el 30 en febrero),
 * la cuota vence el último día de ese mes. La primera cuota vence el mes siguiente al desembolso.
 */
public class FrecuenciaMensual implements Frecuencia {

    private final int diaDeCobro;

    public FrecuenciaMensual(int diaDeCobro) {
        if (diaDeCobro < 1 || diaDeCobro > 31) {
            throw new IllegalArgumentException("El día de cobro debe estar entre 1 y 31");
        }
        this.diaDeCobro = diaDeCobro;
    }

    public int getDiaDeCobro() {
        return diaDeCobro;
    }

    @Override
    public LocalDate fechaVencimiento(LocalDate desembolso, int numeroCuota) {
        if (numeroCuota < 1) {
            throw new IllegalArgumentException("El número de cuota empieza en 1");
        }
        YearMonth mes = YearMonth.from(desembolso).plusMonths(numeroCuota);
        int dia = Math.min(diaDeCobro, mes.lengthOfMonth());
        return mes.atDay(dia);
    }
}
