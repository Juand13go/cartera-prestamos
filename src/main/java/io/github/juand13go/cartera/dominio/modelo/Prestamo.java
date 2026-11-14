package io.github.juand13go.cartera.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.estructuras.Pila;
import io.github.juand13go.cartera.dominio.estructuras.TablaHash;
import io.github.juand13go.cartera.dominio.politicas.AlVencerPlazo;
import io.github.juand13go.cartera.dominio.politicas.InteresCancelacion;
import io.github.juand13go.cartera.dominio.politicas.OrdenImputacion;

/**
 * Un préstamo tipo rédito: cada periodo genera una cuota de interés y el capital
 * solo baja con lo que el cliente pague de más.
 *
 * El cronograma no se genera completo al inicio: crece una cuota a la vez.
 * Siempre hay una sola cuota "actual" (la última de la lista); la siguiente nace
 * el día después de que esa vence.
 *
 * Los pagos son al préstamo, no a una cuota: el préstamo los reparte entre multas,
 * cuotas y capital, en el orden que diga el contrato. Lo que sobra va a capital.
 *
 * Plazo máximo: la última cuota del plazo (24 meses) incluye todo el capital pendiente.
 * El préstamo queda cerrado cuando no queda capital ni cuotas ni multas por pagar.
 *
 * Un préstamo es su contrato más sus pagos en orden: aplicar los mismos pagos en el mismo
 * orden siempre produce el mismo estado. Por eso deshacer es sacar el último pago de la pila
 * y volver a aplicar los demás desde cero.
 *
 * Las condonaciones de multa se guardan aparte (número de cuota → lo que ya estaba pagado)
 * y se vuelven a aplicar a cada cuota cuando nace, para que sobrevivan al recálculo.
 */
public class Prestamo {
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final String cedulaCliente;
    private final Contrato contrato;
    private ListaDoble<Cuota> cronograma;
    private Pila<Pago> pagos;
    private BigDecimal capitalPendiente;
    private final TablaHash<Integer, BigDecimal> condonaciones;

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
        this.condonaciones = new TablaHash<>();
        empezarDesdeElDesembolso();
    }

    /** Deja el préstamo como el día del desembolso: sin pagos, todo el capital y solo la primera cuota. */
    private void empezarDesdeElDesembolso() {
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

    /**
     * Cerrado = no se debe nada: ni capital, ni cuotas, ni multas.
     * Las multas de una cuota cubierta ya no cambian (quedaron congeladas el día en que se cubrió),
     * por eso se consultan con esa fecha.
     */
    public boolean estaCerrado() {
        if (capitalPendiente.compareTo(BigDecimal.ZERO) > 0) {
            return false;
        }
        for (Cuota cuota : cronograma) {
            if (!cuota.estaCubierta()) {
                return false;
            }
            if (saldoMulta(cuota, cuota.getFechaCubierta()).compareTo(BigDecimal.ZERO) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Todo el capital que el cliente todavía debe: el pendiente más el que ya se pasó
     * a la última cuota del plazo y no se ha pagado.
     */
    public BigDecimal capitalAdeudado() {
        BigDecimal total = capitalPendiente;
        for (Cuota cuota : cronograma) {
            total = total.add(cuota.capitalSinPagar());
        }
        return total;
    }

    /** Lo que falta pagar de la multa de una cuota a la fecha {@code hoy}. */
    public BigDecimal saldoMulta(Cuota cuota, LocalDate hoy) {
        BigDecimal multa = cuota.multaACobrar(contrato.multa(cuota, hoy));
        BigDecimal falta = multa.subtract(cuota.getMultaPagada());
        return falta.max(BigDecimal.ZERO);
    }

    /**
     * El prestamista anula lo que falta de la multa de una cuota. Lo ya pagado se respeta.
     * Se anota en la tabla de condonaciones para que sobreviva si luego se deshace un pago.
     */
    public void condonarMulta(int numeroCuota, LocalDate hoy) {
        actualizarCronograma(hoy);
        if (numeroCuota < 1 || numeroCuota > cronograma.tamano()) {
            throw new IllegalArgumentException("No existe la cuota " + numeroCuota);
        }
        Cuota cuota = cronograma.obtener(numeroCuota - 1);
        cuota.condonarMulta(cuota.getMultaPagada());
        condonaciones.guardar(numeroCuota, cuota.getMultaPagada());
    }

    /**
     * Todo lo que se debe a la fecha {@code hoy} con el cronograma actual:
     * multas pendientes + lo pendiente de las cuotas + capital.
     */
    public BigDecimal saldoTotal(LocalDate hoy) {
        BigDecimal total = capitalPendiente;
        for (Cuota cuota : cronograma) {
            total = total.add(cuota.saldoPendiente()).add(saldoMulta(cuota, hoy));
        }
        return total;
    }

    /**
     * Genera las cuotas que ya deberían existir a la fecha {@code hoy}:
     * mientras el préstamo esté activo, la última cuota ya esté vencida y el plazo lo permita, agrega la siguiente.
     */
    public void actualizarCronograma(LocalDate hoy) {
        while (!estaCerrado() && hoy.isAfter(cronograma.ultimo().getFechaVencimiento())
                && quedanCuotasPorGenerar()) {
            agregarSiguienteCuota();
        }
    }

    /**
     * Dentro del plazo siempre hay una cuota más. Después del plazo, solo si el contrato dice
     * SEGUIR_COBRANDO y todavía queda capital sin pagar.
     */
    private boolean quedanCuotasPorGenerar() {
        if (cronograma.tamano() < cuotasDelPlazo()) {
            return true;
        }
        return contrato.getAlVencerPlazo() == AlVencerPlazo.SEGUIR_COBRANDO
                && capitalAdeudado().compareTo(BigDecimal.ZERO) > 0;
    }

    private int cuotasDelPlazo() {
        return contrato.getFrecuencia().cuotasEnPlazoMaximo();
    }

    /**
     * Registra un pago y lo reparte según el orden de imputación del contrato:
     * - MULTAS_PRIMERO: multas → cuotas vencidas → cuota actual → capital;
     * - CUOTAS_PRIMERO: cuotas vencidas → cuota actual → multas → capital.
     * Lo que sobra después de multas y cuotas va a capital.
     */
    public Pago registrarPago(LocalDate fecha, BigDecimal monto) {
        validarFecha(fecha);
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El pago debe ser mayor que cero");
        }
        actualizarCronograma(fecha);
        return aplicarPago(fecha, monto, false);
    }

    /**
     * Cuánto tendría que pagar el cliente para cancelar todo a la fecha {@code hoy}:
     * cuotas vencidas + multas + capital + interés del periodo en curso según el contrato.
     * Nunca se cobran intereses de periodos futuros, porque esas cuotas todavía no existen.
     */
    public BigDecimal valorParaCancelar(LocalDate hoy) {
        actualizarCronograma(hoy);
        if (estaCerrado()) {
            return BigDecimal.ZERO;
        }
        return saldoTotal(hoy).subtract(rebajaPorCancelar(hoy));
    }

    /**
     * El cliente paga todo lo que debe a la fecha (ver {@link #valorParaCancelar}) y el préstamo se cierra.
     * Si el contrato cobra el interés por días, primero se rebaja la cuota en curso y luego se paga como cualquier pago.
     */
    public Pago cancelar(LocalDate fecha) {
        validarFecha(fecha);
        actualizarCronograma(fecha);
        BigDecimal rebaja = rebajaPorCancelar(fecha);
        if (rebaja.compareTo(BigDecimal.ZERO) > 0) {
            cronograma.ultimo().rebajarInteres(rebaja, fecha);
        }
        return aplicarPago(fecha, saldoTotal(fecha), true);
    }

    /**
     * Con POR_DIAS y antes del vencimiento de la cuota en curso: cuánto interés de esa cuota no se cobra.
     * En cualquier otro caso, 0 (se cobra el periodo completo).
     */
    private BigDecimal rebajaPorCancelar(LocalDate hoy) {
        Cuota enCurso = cronograma.ultimo();
        if (contrato.getInteresCancelacion() != InteresCancelacion.POR_DIAS
                || !hoy.isBefore(enCurso.getFechaVencimiento())) {
            return BigDecimal.ZERO;
        }
        return enCurso.rebajaDeInteres(interesCausado(enCurso, hoy));
    }

    /** Interés de la cuota × días corridos del periodo / días del periodo, redondeado al peso. */
    private BigDecimal interesCausado(Cuota cuota, LocalDate hoy) {
        LocalDate inicio;
        if (cuota.getNumero() == 1) {
            inicio = contrato.getFechaDesembolso();
        } else {
            inicio = contrato.fechaVencimiento(cuota.getNumero() - 1);
        }
        long diasDelPeriodo = ChronoUnit.DAYS.between(inicio, cuota.getFechaVencimiento());
        long diasCorridos = ChronoUnit.DAYS.between(inicio, hoy);
        return cuota.getInteres()
                .multiply(BigDecimal.valueOf(diasCorridos))
                .divide(BigDecimal.valueOf(diasDelPeriodo), 0, RoundingMode.HALF_UP);
    }

    /** Reparte un pago ya validado, con el cronograma ya actualizado a su fecha, y lo apila. */
    private Pago aplicarPago(LocalDate fecha, BigDecimal monto, boolean esCancelacion) {
        BigDecimal deuda = saldoTotal(fecha);
        if (monto.compareTo(deuda) > 0) {
            throw new IllegalArgumentException("El pago supera todo lo que se debe: " + deuda);
        }

        BigDecimal restante = monto;
        BigDecimal abonoAMultas;
        BigDecimal abonoACuotas;
        if (contrato.getOrdenImputacion() == OrdenImputacion.MULTAS_PRIMERO) {
            abonoAMultas = pagarMultas(fecha, restante);
            restante = restante.subtract(abonoAMultas);
            abonoACuotas = pagarCuotas(fecha, restante);
            restante = restante.subtract(abonoACuotas);
        } else {
            abonoACuotas = pagarCuotas(fecha, restante);
            restante = restante.subtract(abonoACuotas);
            abonoAMultas = pagarMultas(fecha, restante);
            restante = restante.subtract(abonoAMultas);
        }
        capitalPendiente = capitalPendiente.subtract(restante);

        Pago pago = new Pago(fecha, monto, abonoAMultas, abonoACuotas, restante, esCancelacion);
        pagos.apilar(pago);
        return pago;
    }

    /**
     * Reparte hasta {@code disponible} entre las cuotas pendientes, de la más vieja a la más nueva.
     * Como el cronograma está en orden y la cuota actual es la última, basta recorrerlo de principio a fin.
     * Devuelve cuánto usó.
     */
    private BigDecimal pagarCuotas(LocalDate fecha, BigDecimal disponible) {
        BigDecimal usado = BigDecimal.ZERO;
        for (Cuota cuota : cronograma) {
            BigDecimal queda = disponible.subtract(usado);
            if (queda.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }
            if (cuota.estaCubierta()) {
                continue;
            }
            BigDecimal abono = queda.min(cuota.saldoPendiente());
            cuota.cubrir(abono, fecha);
            usado = usado.add(abono);
        }
        return usado;
    }

    /** Reparte hasta {@code disponible} entre las multas pendientes, de la más vieja a la más nueva. Devuelve cuánto usó. */
    private BigDecimal pagarMultas(LocalDate fecha, BigDecimal disponible) {
        BigDecimal usado = BigDecimal.ZERO;
        for (Cuota cuota : cronograma) {
            BigDecimal queda = disponible.subtract(usado);
            if (queda.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }
            BigDecimal falta = saldoMulta(cuota, fecha);
            if (falta.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal abono = queda.min(falta);
            cuota.abonarMulta(abono);
            usado = usado.add(abono);
        }
        return usado;
    }

    /**
     * Anula el último pago registrado y lo devuelve.
     * Saca el pago de la pila (O(1)) y recalcula todo con los pagos que quedan.
     * El cronograma queda generado hasta la fecha del último pago que queda;
     * para verlo a otra fecha, llamar a {@link #actualizarCronograma(LocalDate)}.
     */
    public Pago deshacerUltimoPago() {
        if (pagos.estaVacia()) {
            throw new IllegalStateException("No hay pagos para deshacer");
        }
        Pago deshecho = pagos.desapilar();
        recalcular();
        return deshecho;
    }

    /**
     * Vuelve a construir el estado desde el contrato aplicando los pagos de la pila, del más viejo al más nuevo.
     * La pila los entrega del más nuevo al más viejo, así que primero se pasan a una lista
     * con agregarAlInicio, que invierte el orden.
     * O(p × n): por cada uno de los p pagos se recorre el cronograma de n cuotas.
     */
    private void recalcular() {
        ListaDoble<Pago> enOrden = new ListaDoble<>();
        for (Pago pago : pagos) {
            enOrden.agregarAlInicio(pago);
        }
        empezarDesdeElDesembolso();
        for (Pago pago : enOrden) {
            if (pago.isCancelacion()) {
                cancelar(pago.getFecha());
            } else {
                registrarPago(pago.getFecha(), pago.getMonto());
            }
        }
    }

    private void validarFecha(LocalDate fecha) {
        if (estaCerrado()) {
            throw new IllegalStateException("El préstamo ya está cerrado");
        }
        if (fecha == null || fecha.isBefore(contrato.getFechaDesembolso())) {
            throw new IllegalArgumentException("El pago no puede ser anterior al desembolso");
        }
        if (!pagos.estaVacia() && fecha.isBefore(pagos.verTope().getFecha())) {
            throw new IllegalArgumentException("El pago no puede ser anterior al último pago registrado");
        }
    }

    /**
     * Agrega al final del cronograma la cuota del periodo siguiente.
     * Si es la última del plazo, además del interés incluye todo el capital pendiente,
     * que pasa del préstamo a la cuota.
     */
    private void agregarSiguienteCuota() {
        int numero = cronograma.tamano() + 1;
        LocalDate vence = contrato.fechaVencimiento(numero);
        BigDecimal interes = interesDelPeriodo();
        Cuota cuota;
        if (numero == cuotasDelPlazo()) {
            cuota = new Cuota(numero, vence, interes, capitalPendiente);
            capitalPendiente = BigDecimal.ZERO;
        } else {
            cuota = new Cuota(numero, vence, interes);
        }
        BigDecimal condonadaEn = condonaciones.buscar(numero);
        if (condonadaEn != null) {
            cuota.condonarMulta(condonadaEn);
        }
        cronograma.agregarAlFinal(cuota);
    }

    /** Base del interés × tasa / 100, redondeado al peso. */
    private BigDecimal interesDelPeriodo() {
        BigDecimal base;
        if (contrato.getBaseInteres() == BaseInteres.CAPITAL_INICIAL) {
            base = contrato.getCapital();
        } else {
            base = capitalAdeudado();
        }
        return base.multiply(contrato.getTasaPorPeriodo()).divide(CIEN, 0, RoundingMode.HALF_UP);
    }
}
