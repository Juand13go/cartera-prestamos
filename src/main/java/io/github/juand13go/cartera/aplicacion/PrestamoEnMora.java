package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;

/** Una línea de la lista de mora: un préstamo con cuotas vencidas sin cubrir. */
public record PrestamoEnMora(
        long prestamoId,
        String cedula,
        String nombreCliente,
        String telefono,
        int diasDeAtraso,
        int cuotasVencidas,
        BigDecimal totalVencido) {
}
