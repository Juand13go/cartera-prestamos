package io.github.juand13go.cartera.dominio.politicas;

import java.math.BigDecimal;

/** Un valor fijo por cada día con multa. Ejemplo: $2.000 diarios. */
public class MultaFijaDiaria implements PoliticaMora {

    private final BigDecimal valorDiario;

    public MultaFijaDiaria(BigDecimal valorDiario) {
        if (valorDiario == null || valorDiario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor diario de la multa debe ser mayor que cero");
        }
        this.valorDiario = valorDiario;
    }

    public BigDecimal getValorDiario() {
        return valorDiario;
    }

    @Override
    public BigDecimal multa(BigDecimal valorCuota, int diasConMulta) {
        return valorDiario.multiply(BigDecimal.valueOf(diasConMulta));
    }
}
