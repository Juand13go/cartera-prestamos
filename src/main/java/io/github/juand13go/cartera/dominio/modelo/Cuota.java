package io.github.juand13go.cartera.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Una cuota del cronograma: lo que el cliente debe pagar en una fecha.
 * El estado no se guarda: se calcula comparando las fechas.
 */
public class Cuota {
    private final int numero;
    private final LocalDate fechaVencimiento;
    private final BigDecimal valor;
    private BigDecimal valorCubierto;
    private LocalDate fechaCubierta;

    public Cuota(int numero, LocalDate fechaVencimiento, BigDecimal valor) {
        this.numero = numero;
        this.fechaVencimiento = fechaVencimiento;
        this.valor = valor;
        this.valorCubierto = BigDecimal.ZERO;
        this.fechaCubierta = null;
    }

    public int getNumero() {
        return numero;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public BigDecimal getValorCubierto() {
        return valorCubierto;
    }

    /** Fecha en que la cuota quedó completa, o null si todavía no lo está. */
    public LocalDate getFechaCubierta() {
        return fechaCubierta;
    }

    /** Lo que falta por pagar de esta cuota. */
    public BigDecimal saldoPendiente() {
        return valor.subtract(valorCubierto);
    }

    public boolean estaCubierta() {
        return valorCubierto.compareTo(valor) >= 0;
    }

    /**
     * Abona a la cuota. Si con este abono queda completa, guarda la fecha.
     * No acepta más de lo que falta: es el préstamo quien reparte el pago entre cuotas.
     */
    public void cubrir(BigDecimal monto, LocalDate fecha) {
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
        if (monto.compareTo(saldoPendiente()) > 0) {
            throw new IllegalArgumentException("El monto supera el saldo pendiente de la cuota");
        }
        valorCubierto = valorCubierto.add(monto);
        if (estaCubierta()) {
            fechaCubierta = fecha;
        }
    }

    /**
     * Días que lleva (o llevó) de atraso: desde el vencimiento hasta el día en que se cubrió,
     * o hasta hoy si todavía no está cubierta. Si no está atrasada, 0.
     * Por eso la multa se congela el día en que la cuota se paga completa.
     */
    public int diasDeAtraso(LocalDate hoy) {
        LocalDate hasta = estaCubierta() ? fechaCubierta : hoy;
        long dias = ChronoUnit.DAYS.between(fechaVencimiento, hasta);
        return (int) Math.max(0, dias);
    }

    public EstadoCuota estado(LocalDate hoy) {
        if (estaCubierta()) {
            if (!fechaCubierta.isAfter(fechaVencimiento)) {
                return EstadoCuota.PAGADA;
            }
            return EstadoCuota.PAGADA_CON_RETRASO;
        }
        if (!hoy.isAfter(fechaVencimiento)) {
            return EstadoCuota.CON_TIEMPO;
        }
        return EstadoCuota.MORA;
    }
}
