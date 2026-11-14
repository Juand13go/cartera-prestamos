package io.github.juand13go.cartera.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que el cliente entregó en una fecha, y cómo lo repartió el préstamo.
 * Se cumple siempre: monto = abonoAMultas + abonoACuotas + abonoACapital.
 */
public class Pago {
    private final LocalDate fecha;
    private final BigDecimal monto;
    private final BigDecimal abonoAMultas;
    private final BigDecimal abonoACuotas;
    private final BigDecimal abonoACapital;

    public Pago(LocalDate fecha, BigDecimal monto, BigDecimal abonoAMultas, BigDecimal abonoACuotas,
                BigDecimal abonoACapital) {
        this.fecha = fecha;
        this.monto = monto;
        this.abonoAMultas = abonoAMultas;
        this.abonoACuotas = abonoACuotas;
        this.abonoACapital = abonoACapital;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    /** Parte del pago que fue a multas por atraso. */
    public BigDecimal getAbonoAMultas() {
        return abonoAMultas;
    }

    /** Parte del pago que cubrió cuotas (interés). */
    public BigDecimal getAbonoACuotas() {
        return abonoACuotas;
    }

    /** Parte del pago que bajó el capital. */
    public BigDecimal getAbonoACapital() {
        return abonoACapital;
    }
}
