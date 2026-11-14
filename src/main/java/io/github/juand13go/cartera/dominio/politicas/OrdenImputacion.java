package io.github.juand13go.cartera.dominio.politicas;

/**
 * En qué orden se reparte un pago. En los dos casos las cuotas van de la más vieja a la más nueva,
 * las multas también, y el capital siempre va de último.
 */
public enum OrdenImputacion {
    /** Multas → cuotas vencidas → cuota actual → capital. */
    MULTAS_PRIMERO,
    /** Cuotas vencidas → cuota actual → multas → capital. */
    CUOTAS_PRIMERO
}
