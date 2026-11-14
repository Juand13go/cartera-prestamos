package io.github.juand13go.cartera.dominio.politicas;

/** Cuánto interés del periodo en curso se cobra si el cliente cancela todo antes del vencimiento. */
public enum InteresCancelacion {
    /** Se cobra el interés completo del periodo en curso. */
    PERIODO_COMPLETO,
    /** Se cobra el interés del periodo × días corridos / días del periodo. */
    POR_DIAS
}
