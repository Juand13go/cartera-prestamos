package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;

import io.github.juand13go.cartera.dominio.modelo.Pago;

/**
 * Qué pasaría si el cliente abonara hoy cierto monto. Nada de esto se guarda.
 *
 * @param pago            cómo se repartiría el abono (multas, cuotas, capital)
 * @param interesAntes    interés de la próxima cuota sin el abono
 * @param interesDespues  interés de la próxima cuota con el abono (solo cambia con capital vigente)
 * @param baseVigente     true si el interés se calcula sobre el capital vigente
 */
public record ResultadoSimulacion(
        Pago pago,
        BigDecimal capitalAntes,
        BigDecimal capitalDespues,
        BigDecimal interesAntes,
        BigDecimal interesDespues,
        boolean baseVigente) {
}
