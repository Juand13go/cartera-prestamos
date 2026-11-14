package io.github.juand13go.cartera.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.juand13go.cartera.dominio.politicas.Frecuencia;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaMensual;

/**
 * Las condiciones del préstamo. Se fijan al crearlo y no cambian después: por eso no tiene setters
 * y todos sus atributos son final.
 *
 * Se crea en dos pasos, como un contrato de papel: primero se llena un {@link Borrador}
 * (que ya trae los valores por defecto) y luego se firma.
 * <pre>
 * Contrato contrato = Contrato.borrador(capital, tasa, desembolso)
 *         .base(BaseInteres.CAPITAL_VIGENTE)
 *         .firmar();
 * </pre>
 */
public class Contrato {
    private final BigDecimal capital;
    private final BigDecimal tasaPorPeriodo;
    private final BaseInteres baseInteres;
    private final LocalDate fechaDesembolso;
    private final Frecuencia frecuencia;

    /**
     * Empieza un borrador con lo que no tiene valor por defecto.
     *
     * @param capital         lo que se presta
     * @param tasaPorPeriodo  porcentaje nominal por periodo; 8 significa 8 %
     * @param fechaDesembolso el día en que se entrega el dinero
     */
    public static Borrador borrador(BigDecimal capital, BigDecimal tasaPorPeriodo, LocalDate fechaDesembolso) {
        return new Borrador(capital, tasaPorPeriodo, fechaDesembolso);
    }

    /** Solo se llega aquí desde {@link Borrador#firmar()}; aquí se valida todo. */
    private Contrato(Borrador borrador) {
        if (borrador.capital == null || borrador.capital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El capital debe ser mayor que cero");
        }
        if (borrador.tasaPorPeriodo == null || borrador.tasaPorPeriodo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La tasa debe ser mayor que cero");
        }
        if (borrador.baseInteres == null || borrador.fechaDesembolso == null || borrador.frecuencia == null) {
            throw new IllegalArgumentException("Faltan condiciones del contrato");
        }
        this.capital = borrador.capital;
        this.tasaPorPeriodo = borrador.tasaPorPeriodo;
        this.baseInteres = borrador.baseInteres;
        this.fechaDesembolso = borrador.fechaDesembolso;
        this.frecuencia = borrador.frecuencia;
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

    /**
     * Contrato todavía sin firmar: se puede cambiar cualquier condición.
     * Cada método devuelve el mismo borrador para poder encadenar las llamadas.
     * Los valores por defecto viven aquí, en un solo lugar.
     */
    public static class Borrador {
        private final BigDecimal capital;
        private final BigDecimal tasaPorPeriodo;
        private final LocalDate fechaDesembolso;
        private BaseInteres baseInteres = BaseInteres.CAPITAL_INICIAL;
        private Frecuencia frecuencia;

        private Borrador(BigDecimal capital, BigDecimal tasaPorPeriodo, LocalDate fechaDesembolso) {
            this.capital = capital;
            this.tasaPorPeriodo = tasaPorPeriodo;
            this.fechaDesembolso = fechaDesembolso;
            // Por defecto: mensual, el mismo día del mes del desembolso.
            if (fechaDesembolso != null) {
                this.frecuencia = new FrecuenciaMensual(fechaDesembolso.getDayOfMonth());
            }
        }

        public Borrador base(BaseInteres baseInteres) {
            this.baseInteres = baseInteres;
            return this;
        }

        public Borrador frecuencia(Frecuencia frecuencia) {
            this.frecuencia = frecuencia;
            return this;
        }

        /** Crea el contrato definitivo; a partir de aquí ya no se puede cambiar. */
        public Contrato firmar() {
            return new Contrato(this);
        }
    }
}
