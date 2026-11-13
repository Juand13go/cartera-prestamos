package io.github.juand13go.cartera.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.juand13go.cartera.dominio.politicas.Frecuencia;

/**
 * Las condiciones del préstamo. Se fijan al crearlo y no cambian después: por eso no tiene setters
 * y todos sus atributos son final.
 */
public class Contrato {
    private final BigDecimal capital;
    private final BigDecimal tasaPorPeriodo;
    private final BaseInteres baseInteres;
    private final LocalDate fechaDesembolso;
    private final Frecuencia frecuencia;

    /**
     * @param capital         lo que se presta
     * @param tasaPorPeriodo  porcentaje nominal por periodo; 8 significa 8 %
     * @param baseInteres     sobre qué capital se calcula cada cuota
     * @param fechaDesembolso el día en que se entrega el dinero
     * @param frecuencia      cada cuánto se cobra y en qué día
     */
    public Contrato(BigDecimal capital, BigDecimal tasaPorPeriodo, BaseInteres baseInteres,
                    LocalDate fechaDesembolso, Frecuencia frecuencia) {
        if (capital == null || capital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El capital debe ser mayor que cero");
        }
        if (tasaPorPeriodo == null || tasaPorPeriodo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La tasa debe ser mayor que cero");
        }
        if (baseInteres == null || fechaDesembolso == null || frecuencia == null) {
            throw new IllegalArgumentException("Faltan condiciones del contrato");
        }
        this.capital = capital;
        this.tasaPorPeriodo = tasaPorPeriodo;
        this.baseInteres = baseInteres;
        this.fechaDesembolso = fechaDesembolso;
        this.frecuencia = frecuencia;
    }

    public BigDecimal getCapital() {
        return capital;
    }

    public BigDecimal getTasaPorPeriodo() {
        return tasaPorPeriodo;
    }

    public BaseInteres getBaseInteres() {
        return baseInteres;
    }

    public LocalDate getFechaDesembolso() {
        return fechaDesembolso;
    }

    public Frecuencia getFrecuencia() {
        return frecuencia;
    }
}
