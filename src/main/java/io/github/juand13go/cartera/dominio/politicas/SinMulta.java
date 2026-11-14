package io.github.juand13go.cartera.dominio.politicas;

import java.math.BigDecimal;

/** El atraso no genera multa. */
public class SinMulta implements PoliticaMora {

    @Override
    public BigDecimal multa(BigDecimal valorCuota, int diasConMulta) {
        return BigDecimal.ZERO;
    }
}
