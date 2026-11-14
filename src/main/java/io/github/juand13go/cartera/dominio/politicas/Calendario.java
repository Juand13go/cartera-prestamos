package io.github.juand13go.cartera.dominio.politicas;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Qué días sirven para cobrar. Se aplica a la fecha que calcula la frecuencia. */
public enum Calendario {
    /** Todos los días cuentan: la fecha queda como la calculó la frecuencia. */
    TODOS_LOS_DIAS,
    /** Si la fecha cae domingo, el cobro pasa al lunes. */
    SIN_DOMINGOS;

    public LocalDate ajustar(LocalDate fecha) {
        if (this == SIN_DOMINGOS && fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return fecha.plusDays(1);
        }
        return fecha;
    }
}
