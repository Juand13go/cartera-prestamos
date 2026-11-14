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
    private final BigDecimal capitalIncluido;
    private BigDecimal valorCubierto;
    private LocalDate fechaCubierta;
    private BigDecimal multaPagada;
    private BigDecimal multaCondonadaEn;

    /** Cuota normal del rédito: solo interés. */
    public Cuota(int numero, LocalDate fechaVencimiento, BigDecimal valor) {
        this(numero, fechaVencimiento, valor, BigDecimal.ZERO);
    }

    /**
     * Cuota que además del interés incluye capital (la última del plazo).
     * Su valor es interés + capital.
     */
    public Cuota(int numero, LocalDate fechaVencimiento, BigDecimal interes, BigDecimal capitalIncluido) {
        this.numero = numero;
        this.fechaVencimiento = fechaVencimiento;
        this.valor = interes.add(capitalIncluido);
        this.capitalIncluido = capitalIncluido;
        this.valorCubierto = BigDecimal.ZERO;
        this.fechaCubierta = null;
        this.multaPagada = BigDecimal.ZERO;
        this.multaCondonadaEn = null;
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

    /** Parte del valor que es capital; 0 en todas las cuotas menos la última del plazo. */
    public BigDecimal getCapitalIncluido() {
        return capitalIncluido;
    }

    /** Parte del valor que es interés. */
    public BigDecimal getInteres() {
        return valor.subtract(capitalIncluido);
    }

    /**
     * Cuánto del capital incluido falta por pagar. Dentro de la cuota se paga primero el interés,
     * así que lo que falta es capital hasta donde alcance.
     */
    public BigDecimal capitalSinPagar() {
        return saldoPendiente().min(capitalIncluido);
    }

    public BigDecimal getValorCubierto() {
        return valorCubierto;
    }

    /** Fecha en que la cuota quedó completa, o null si todavía no lo está. */
    public LocalDate getFechaCubierta() {
        return fechaCubierta;
    }

    /** Cuánto se ha abonado a la multa de esta cuota. La multa total la calcula el contrato. */
    public BigDecimal getMultaPagada() {
        return multaPagada;
    }

    /**
     * Abona a la multa de esta cuota. Quien llama (el préstamo) se asegura de no pasarse
     * de lo que falta, porque la multa total depende de la fecha y de la política del contrato.
     */
    public void abonarMulta(BigDecimal monto) {
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
        multaPagada = multaPagada.add(monto);
    }

    public boolean estaMultaCondonada() {
        return multaCondonadaEn != null;
    }

    /**
     * Condona la multa: desde ahora la multa queda fija en {@code loYaPagado}.
     * Lo que ya se había pagado se respeta; lo que faltaba (y lo que se iba a sumar) se anula.
     */
    public void condonarMulta(BigDecimal loYaPagado) {
        if (estaMultaCondonada()) {
            throw new IllegalStateException("La multa de la cuota " + numero + " ya fue condonada");
        }
        multaCondonadaEn = loYaPagado;
    }

    /**
     * De la multa que calcula el contrato, cuánto se cobra:
     * toda si no fue condonada; si fue condonada, como máximo lo que ya estaba pagado al condonar.
     */
    public BigDecimal multaACobrar(BigDecimal multaCalculada) {
        if (estaMultaCondonada()) {
            return multaCalculada.min(multaCondonadaEn);
        }
        return multaCalculada;
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
