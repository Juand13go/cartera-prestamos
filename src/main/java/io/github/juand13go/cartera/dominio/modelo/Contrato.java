package io.github.juand13go.cartera.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.juand13go.cartera.dominio.politicas.Calendario;
import io.github.juand13go.cartera.dominio.politicas.Frecuencia;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaMensual;
import io.github.juand13go.cartera.dominio.politicas.PoliticaMora;
import io.github.juand13go.cartera.dominio.politicas.SinMulta;

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
    private final Calendario calendario;
    private final PoliticaMora politicaMora;
    private final int diasDeGracia;

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
        if (borrador.baseInteres == null || borrador.fechaDesembolso == null || borrador.frecuencia == null
                || borrador.calendario == null || borrador.politicaMora == null) {
            throw new IllegalArgumentException("Faltan condiciones del contrato");
        }
        if (borrador.diasDeGracia < 0) {
            throw new IllegalArgumentException("Los días de gracia no pueden ser negativos");
        }
        this.capital = borrador.capital;
        this.tasaPorPeriodo = borrador.tasaPorPeriodo;
        this.baseInteres = borrador.baseInteres;
        this.fechaDesembolso = borrador.fechaDesembolso;
        this.frecuencia = borrador.frecuencia;
        this.calendario = borrador.calendario;
        this.politicaMora = borrador.politicaMora;
        this.diasDeGracia = borrador.diasDeGracia;
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

    public Calendario getCalendario() {
        return calendario;
    }

    public PoliticaMora getPoliticaMora() {
        return politicaMora;
    }

    public int getDiasDeGracia() {
        return diasDeGracia;
    }

    /** Fecha en que vence la cuota número {@code numeroCuota}: la de la frecuencia, ajustada al calendario. */
    public LocalDate fechaVencimiento(int numeroCuota) {
        return calendario.ajustar(frecuencia.fechaVencimiento(fechaDesembolso, numeroCuota));
    }

    /**
     * Multa total que ha generado una cuota hasta {@code hoy}, según la política de mora.
     * La multa empieza a contar cuando terminan los días de gracia.
     */
    public BigDecimal multa(Cuota cuota, LocalDate hoy) {
        int diasConMulta = Math.max(0, cuota.diasDeAtraso(hoy) - diasDeGracia);
        return politicaMora.multa(cuota.getValor(), diasConMulta);
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
        private Calendario calendario = Calendario.TODOS_LOS_DIAS;
        private PoliticaMora politicaMora = new SinMulta();
        private int diasDeGracia = 2;

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

        public Borrador calendario(Calendario calendario) {
            this.calendario = calendario;
            return this;
        }

        public Borrador mora(PoliticaMora politicaMora) {
            this.politicaMora = politicaMora;
            return this;
        }

        public Borrador diasDeGracia(int diasDeGracia) {
            this.diasDeGracia = diasDeGracia;
            return this;
        }

        /** Crea el contrato definitivo; a partir de aquí ya no se puede cambiar. */
        public Contrato firmar() {
            return new Contrato(this);
        }
    }
}
