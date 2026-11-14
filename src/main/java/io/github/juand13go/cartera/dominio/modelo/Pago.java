package io.github.juand13go.cartera.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;

/**
 * Lo que el cliente entregó en una fecha, y cómo lo repartió el préstamo.
 * Se cumple siempre: monto = abonoAMultas + abonoACuotas + abonoACapital.
 * También recuerda lo que el recibo necesita: qué cuotas quedaron cubiertas con este pago
 * y cuánto se seguía debiendo justo después.
 */
public class Pago {
    private final LocalDate fecha;
    private final BigDecimal monto;
    private final BigDecimal abonoAMultas;
    private final BigDecimal abonoACuotas;
    private final BigDecimal abonoACapital;
    private final boolean cancelacion;
    private final ListaDoble<Integer> cuotasCubiertas;
    private final BigDecimal saldoDespues;

    public Pago(LocalDate fecha, BigDecimal monto, BigDecimal abonoAMultas, BigDecimal abonoACuotas,
                BigDecimal abonoACapital, boolean cancelacion, ListaDoble<Integer> cuotasCubiertas,
                BigDecimal saldoDespues) {
        this.fecha = fecha;
        this.monto = monto;
        this.abonoAMultas = abonoAMultas;
        this.abonoACuotas = abonoACuotas;
        this.abonoACapital = abonoACapital;
        this.cancelacion = cancelacion;
        this.cuotasCubiertas = cuotasCubiertas;
        this.saldoDespues = saldoDespues;
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

    /** true si fue el pago con el que el cliente canceló todo el préstamo. */
    public boolean isCancelacion() {
        return cancelacion;
    }

    /** Números de las cuotas que quedaron completas con este pago (puede estar vacía). */
    public ListaDoble<Integer> getCuotasCubiertas() {
        return cuotasCubiertas;
    }

    /** Todo lo que se seguía debiendo justo después de este pago (multas + cuotas + capital). */
    public BigDecimal getSaldoDespues() {
        return saldoDespues;
    }
}
