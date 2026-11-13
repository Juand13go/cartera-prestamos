package io.github.juand13go.cartera.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.estructuras.Pila;

/**
 * Un préstamo tipo rédito: cada periodo genera una cuota de interés y el capital
 * solo baja con lo que el cliente pague de más.
 *
 * El cronograma no se genera completo al inicio: crece una cuota a la vez.
 * Siempre hay una sola cuota "actual" (la última de la lista); la siguiente nace
 * el día después de que esa vence.
 *
 * Los pagos son al préstamo, no a una cuota: el préstamo los reparte.
 * Cuando el capital pendiente llega a 0, el préstamo queda cerrado.
 */
public class Prestamo {
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final String cedulaCliente;
    private final Contrato contrato;
    private final ListaDoble<Cuota> cronograma;
    private final Pila<Pago> pagos;
    private BigDecimal capitalPendiente;

    /** Crea el préstamo el día del desembolso, con su primera cuota ya generada. */
    public Prestamo(String cedulaCliente, Contrato contrato) {
        if (cedulaCliente == null || cedulaCliente.isBlank()) {
            throw new IllegalArgumentException("El préstamo necesita la cédula del cliente");
        }
        if (contrato == null) {
            throw new IllegalArgumentException("El préstamo necesita un contrato");
        }
        this.cedulaCliente = cedulaCliente;
        this.contrato = contrato;
        this.cronograma = new ListaDoble<>();
        this.pagos = new Pila<>();
        this.capitalPendiente = contrato.getCapital();
        agregarSiguienteCuota();
    }

    public String getCedulaCliente() {
        return cedulaCliente;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public ListaDoble<Cuota> getCronograma() {
        return cronograma;
    }

    public BigDecimal getCapitalPendiente() {
        return capitalPendiente;
    }

    /** Historial de pagos; el de arriba es el último. */
    public Pila<Pago> getPagos() {
        return pagos;
    }

    public boolean estaCerrado() {
        return capitalPendiente.compareTo(BigDecimal.ZERO) == 0;
    }

    /** Todo lo que se debe con el cronograma actual: lo pendiente de las cuotas más el capital. */
    public BigDecimal saldoTotal() {
        BigDecimal total = capitalPendiente;
        for (Cuota cuota : cronograma) {
            total = total.add(cuota.saldoPendiente());
        }
        return total;
    }

    /**
     * Genera las cuotas que ya deberían existir a la fecha {@code hoy}:
     * mientras el préstamo esté activo y la última cuota ya esté vencida, agrega la siguiente.
     */
    public void actualizarCronograma(LocalDate hoy) {
        while (!estaCerrado() && hoy.isAfter(cronograma.ultimo().getFechaVencimiento())) {
            agregarSiguienteCuota();
        }
    }

    /**
     * Registra un pago y lo reparte en este orden:
     * cuotas vencidas (de la más vieja a la más nueva) → cuota actual → capital.
     * Como el cronograma está en orden y la cuota actual es la última, basta recorrerlo de principio a fin.
     */
    public Pago registrarPago(LocalDate fecha, BigDecimal monto) {
        validarPago(fecha, monto);
        actualizarCronograma(fecha);
        if (monto.compareTo(saldoTotal()) > 0) {
            throw new IllegalArgumentException("El pago supera todo lo que se debe: " + saldoTotal());
        }

        BigDecimal restante = monto;
        BigDecimal abonoACuotas = BigDecimal.ZERO;
        for (Cuota cuota : cronograma) {
            if (restante.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }
            if (cuota.estaCubierta()) {
                continue;
            }
            BigDecimal abono = restante.min(cuota.saldoPendiente());
            cuota.cubrir(abono, fecha);
            abonoACuotas = abonoACuotas.add(abono);
            restante = restante.subtract(abono);
        }
        capitalPendiente = capitalPendiente.subtract(restante);

        Pago pago = new Pago(fecha, monto, abonoACuotas, restante);
        pagos.apilar(pago);
        return pago;
    }

    private void validarPago(LocalDate fecha, BigDecimal monto) {
        if (estaCerrado()) {
            throw new IllegalStateException("El préstamo ya está cerrado");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El pago debe ser mayor que cero");
        }
        if (fecha == null || fecha.isBefore(contrato.getFechaDesembolso())) {
            throw new IllegalArgumentException("El pago no puede ser anterior al desembolso");
        }
        if (!pagos.estaVacia() && fecha.isBefore(pagos.verTope().getFecha())) {
            throw new IllegalArgumentException("El pago no puede ser anterior al último pago registrado");
        }
    }

    /** Agrega al final del cronograma la cuota del periodo siguiente. */
    private void agregarSiguienteCuota() {
        int numero = cronograma.tamano() + 1;
        LocalDate vence = contrato.getFrecuencia().fechaVencimiento(contrato.getFechaDesembolso(), numero);
        cronograma.agregarAlFinal(new Cuota(numero, vence, interesDelPeriodo()));
    }

    /** Base del interés × tasa / 100, redondeado al peso. */
    private BigDecimal interesDelPeriodo() {
        BigDecimal base;
        if (contrato.getBaseInteres() == BaseInteres.CAPITAL_INICIAL) {
            base = contrato.getCapital();
        } else {
            base = capitalPendiente;
        }
        return base.multiply(contrato.getTasaPorPeriodo()).divide(CIEN, 0, RoundingMode.HALF_UP);
    }
}
