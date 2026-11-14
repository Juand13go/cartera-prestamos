package io.github.juand13go.cartera.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;

/**
 * Comprobante de un pago: número, fecha y hora, valor, desglose, cuotas cubiertas y saldo restante.
 * No guarda nada propio: junta el pago (lo que calculó el préstamo) con su número y hora de registro.
 */
public class Recibo {
    private final long numero;
    private final LocalDateTime fechaHora;
    private final long prestamoId;
    private final Cliente cliente;
    private final Pago pago;

    /**
     * @param numero    número único del recibo (el id del pago en la base de datos)
     * @param fechaHora cuándo se registró el pago
     */
    public Recibo(long numero, LocalDateTime fechaHora, long prestamoId, Cliente cliente, Pago pago) {
        this.numero = numero;
        this.fechaHora = fechaHora;
        this.prestamoId = prestamoId;
        this.cliente = cliente;
        this.pago = pago;
    }

    public long getNumero() {
        return numero;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public long getPrestamoId() {
        return prestamoId;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Pago getPago() {
        return pago;
    }

    public BigDecimal getValor() {
        return pago.getMonto();
    }

    public ListaDoble<Integer> getCuotasCubiertas() {
        return pago.getCuotasCubiertas();
    }

    public BigDecimal getSaldoRestante() {
        return pago.getSaldoDespues();
    }
}
