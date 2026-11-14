package io.github.juand13go.cartera.dominio.politicas;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Un porcentaje sobre el valor de la cuota, cobrado una sola vez apenas empieza la multa. */
public class PorcentajeUnico implements PoliticaMora {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final BigDecimal porcentaje;

    public PorcentajeUnico(BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El porcentaje de la multa debe ser mayor que cero");
        }
        this.porcentaje = porcentaje;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    @Override
    public BigDecimal multa(BigDecimal valorCuota, int diasConMulta) {
        if (diasConMulta == 0) {
            return BigDecimal.ZERO;
        }
        return valorCuota.multiply(porcentaje).divide(CIEN, 0, RoundingMode.HALF_UP);
    }
}
