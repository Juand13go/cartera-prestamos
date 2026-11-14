package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.juand13go.cartera.dominio.estructuras.ColaPrioridad;

/**
 * Lo que ve el prestamista al entrar: la agenda de cobros y la lista de mora, cada una en su cola de prioridad.
 * Al recorrerlas con for-each (o th:each) salen en orden de prioridad sin vaciarse.
 * Los totales alimentan las tarjetas de resumen de arriba del panel:
 * - cobrosDeHoy / totalCobrosDeHoy: cuotas que vencen hoy y lo que falta de ellas;
 * - totalEnMora: suma del total vencido (cuotas y multas) de la lista de mora;
 * - prestamosActivos / carteraTotal: préstamos sin cerrar y el capital que todavía deben.
 */
public record Panel(LocalDate hoy, ColaPrioridad<CobroAgendado> agenda, ColaPrioridad<PrestamoEnMora> mora,
                    int cobrosDeHoy, BigDecimal totalCobrosDeHoy, BigDecimal totalEnMora,
                    int prestamosActivos, BigDecimal carteraTotal) {
}
