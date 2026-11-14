package io.github.juand13go.cartera.dominio.politicas;

/** Qué pasa si se llega al final del plazo máximo y el cliente no ha pagado todo. */
public enum AlVencerPlazo {
    /** No se generan más cuotas: la última (que incluye el capital) queda en mora y genera multa. */
    TRATAR_COMO_MORA,
    /** Se sigue generando una cuota de interés por periodo mientras quede capital sin pagar. */
    SEGUIR_COBRANDO
}
