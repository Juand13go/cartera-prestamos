package io.github.juand13go.cartera.dominio.politicas;

import java.math.BigDecimal;

/**
 * Cómo se calcula la multa de una cuota atrasada (patrón Strategy: una clase por cada opción).
 * Los días de gracia ya vienen descontados: aquí solo llegan los días que sí tienen multa.
 */
public interface PoliticaMora {

    /**
     * Multa total acumulada de una cuota, redondeada al peso.
     *
     * @param valorCuota   valor de la cuota atrasada
     * @param diasConMulta días de atraso que ya pasaron los días de gracia (0 o más)
     */
    BigDecimal multa(BigDecimal valorCuota, int diasConMulta);
}
