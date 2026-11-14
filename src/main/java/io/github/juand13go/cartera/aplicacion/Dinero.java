package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;

/** Regla común a los casos de uso: el dinero se maneja al peso, sin centavos. */
final class Dinero {

    private Dinero() {
    }

    /** Lanza IllegalArgumentException si el monto tiene centavos (por ejemplo 1000.50). */
    static void validarSinCentavos(BigDecimal monto, String queEs) {
        if (monto != null && monto.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(queEs + " debe ser en pesos, sin centavos");
        }
    }
}
