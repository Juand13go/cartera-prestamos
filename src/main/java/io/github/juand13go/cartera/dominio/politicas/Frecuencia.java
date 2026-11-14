package io.github.juand13go.cartera.dominio.politicas;

import java.time.LocalDate;

/**
 * Cada cuánto se cobra un préstamo (patrón Strategy: una clase por cada opción).
 * Cada opción sabe calcular la fecha de vencimiento de cualquier cuota.
 */
public interface Frecuencia {

    /**
     * Fecha en que vence la cuota número {@code numeroCuota} (la primera es la 1).
     * Se calcula desde el desembolso y no desde la cuota anterior, para que un ajuste
     * en un mes (por ejemplo, febrero) no se arrastre a los siguientes.
     */
    LocalDate fechaVencimiento(LocalDate desembolso, int numeroCuota);

    /** Cuántas cuotas caben en el plazo máximo de 24 meses con esta frecuencia. La última incluye el capital. */
    int cuotasEnPlazoMaximo();
}
