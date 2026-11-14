package io.github.juand13go.cartera.dominio.politicas;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Un porcentaje sobre el valor de la cuota por cada día con multa. */
public class PorcentajeDiario implements PoliticaMora {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final BigDecimal porcentaje;

    public PorcentajeDiario(BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El porcentaje de la multa debe ser mayor que cero");
        }
        this.porcentaje = porcentaje;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    /** Se multiplica todo primero y se redondea una sola vez al final, para no acumular errores de redondeo. */
    @Override
    public BigDecimal multa(BigDecimal valorCuota, int diasConMulta) {
        return valorCuota.multiply(porcentaje)
                .multiply(BigDecimal.valueOf(diasConMulta))
                .divide(CIEN, 0, RoundingMode.HALF_UP);
    }
}
