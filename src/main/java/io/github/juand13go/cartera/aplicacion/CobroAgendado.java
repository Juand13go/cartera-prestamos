package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.juand13go.cartera.dominio.politicas.Frecuencia;

/**
 * Una línea de la agenda: la próxima cuota de un préstamo que se debe cobrar pronto.
 * Trae la frecuencia del contrato para que la vista pueda decir si el préstamo es semanal o quincenal.
 */
public record CobroAgendado(
        long prestamoId,
        String cedula,
        String nombreCliente,
        String telefono,
        int numeroCuota,
        LocalDate fecha,
        BigDecimal valorPendiente,
        boolean esHoy,
        Frecuencia frecuencia) {
}
