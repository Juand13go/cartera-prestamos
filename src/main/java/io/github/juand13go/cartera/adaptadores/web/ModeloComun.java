package io.github.juand13go.cartera.adaptadores.web;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Datos que necesitan todas las vistas. Hoy, solo la fecha del reloj del sistema (real o simulada). */
@ControllerAdvice
public class ModeloComun {

    private final Clock reloj;

    public ModeloComun(Clock reloj) {
        this.reloj = reloj;
    }

    @ModelAttribute("hoy")
    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }
}
