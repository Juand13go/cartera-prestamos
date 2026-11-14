package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Una línea de la agenda: la próxima cuota de un préstamo que se debe cobrar pronto. */
public record CobroAgendado(
        long prestamoId,
        String cedula,
        String nombreCliente,
        String telefono,
        int numeroCuota,
        LocalDate fecha,
        BigDecimal valorPendiente,
        boolean esHoy) {
}
