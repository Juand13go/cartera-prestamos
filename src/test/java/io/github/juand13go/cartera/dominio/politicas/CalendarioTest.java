package io.github.juand13go.cartera.dominio.politicas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CalendarioTest {

    /** El 10 de enero de 2027 es domingo. */
    private static final LocalDate DOMINGO = LocalDate.of(2027, 1, 10);

    @Test
    void laFechaDePruebaEsDomingo() {
        assertEquals(DayOfWeek.SUNDAY, DOMINGO.getDayOfWeek());
    }

    @Test
    void sinDomingosUnDomingoPasaAlLunes() {
        assertEquals(LocalDate.of(2027, 1, 11), Calendario.SIN_DOMINGOS.ajustar(DOMINGO));
    }

    @Test
    void sinDomingosLosDemasDiasNoCambian() {
        LocalDate sabado = LocalDate.of(2027, 1, 9);
        LocalDate lunes = LocalDate.of(2027, 1, 11);

        assertEquals(sabado, Calendario.SIN_DOMINGOS.ajustar(sabado));
        assertEquals(lunes, Calendario.SIN_DOMINGOS.ajustar(lunes));
    }

    @Test
    void conTodosLosDiasNingunaFechaCambia() {
        assertEquals(DOMINGO, Calendario.TODOS_LOS_DIAS.ajustar(DOMINGO));
    }
}
