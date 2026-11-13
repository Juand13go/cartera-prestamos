package io.github.juand13go.cartera.dominio.modelo;

/** Sobre qué capital se calcula el interés de cada cuota. */
public enum BaseInteres {
    /** Siempre sobre el capital prestado: el interés nunca baja aunque el cliente abone a capital. */
    CAPITAL_INICIAL,
    /** Sobre el capital pendiente al momento de generar la cuota: si el cliente abona a capital, el interés baja. */
    CAPITAL_VIGENTE
}
