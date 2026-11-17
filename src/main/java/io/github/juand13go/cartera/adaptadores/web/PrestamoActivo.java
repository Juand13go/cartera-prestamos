package io.github.juand13go.cartera.adaptadores.web;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Una fila de la tabla de préstamos activos: el préstamo con el nombre de su cliente y sus saldos de hoy. */
public record PrestamoActivo(
        long prestamoId,
        String cedula,
        String nombreCliente,
        LocalDate fechaDesembolso,
        BigDecimal capitalPrestado,
        BigDecimal capitalAdeudado,
        BigDecimal saldoHoy) {
}
