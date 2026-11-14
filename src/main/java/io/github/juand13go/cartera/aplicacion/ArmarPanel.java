package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

import io.github.juand13go.cartera.dominio.estructuras.ColaPrioridad;
import io.github.juand13go.cartera.dominio.modelo.Cliente;
import io.github.juand13go.cartera.dominio.modelo.Cuota;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/**
 * Caso de uso: arma el panel del prestamista con dos colas de prioridad.
 * - Agenda: la próxima cuota sin vencer de cada préstamo activo que vence en los próximos días;
 *   sale primero la fecha más cercana.
 * - Mora: los préstamos con cuotas vencidas; sale primero el de más días de atraso.
 */
public class ArmarPanel {

    /** Cuántos días hacia adelante muestra la agenda (hoy incluido). */
    static final int DIAS_DE_AGENDA = 7;

    /** La agenda ordena por fecha y, si coincide, por nombre del cliente. */
    private static final Comparator<CobroAgendado> POR_FECHA =
            Comparator.comparing(CobroAgendado::fecha).thenComparing(CobroAgendado::nombreCliente);

    /** La mora ordena por días de atraso al revés: el más atrasado primero. */
    private static final Comparator<PrestamoEnMora> MAS_ATRASADO_PRIMERO =
            Comparator.comparingInt(PrestamoEnMora::diasDeAtraso).reversed();

    private final ConsultarPrestamos consultar;
    private final DirectorioClientes directorio;

    public ArmarPanel(ConsultarPrestamos consultar, DirectorioClientes directorio) {
        this.consultar = consultar;
        this.directorio = directorio;
    }

    /**
     * Recorre los préstamos una vez: mete cada uno en la cola que le corresponda y, en el mismo recorrido,
     * suma los totales de las tarjetas de resumen. O(P × n + P log P).
     */
    public Panel ejecutar() {
        LocalDate hoy = consultar.hoy();
        ColaPrioridad<CobroAgendado> agenda = new ColaPrioridad<>(POR_FECHA);
        ColaPrioridad<PrestamoEnMora> mora = new ColaPrioridad<>(MAS_ATRASADO_PRIMERO);
        int cobrosDeHoy = 0;
        BigDecimal totalCobrosDeHoy = BigDecimal.ZERO;
        BigDecimal totalEnMora = BigDecimal.ZERO;
        int prestamosActivos = 0;
        BigDecimal carteraTotal = BigDecimal.ZERO;

        for (Prestamo prestamo : consultar.todos()) {
            if (prestamo.estaCerrado()) {
                continue;
            }
            prestamosActivos++;
            carteraTotal = carteraTotal.add(prestamo.capitalAdeudado());
            Cliente cliente = directorio.buscar(prestamo.getCedulaCliente());
            CobroAgendado cobro = proximoCobro(prestamo, cliente, hoy);
            if (cobro != null) {
                agenda.insertar(cobro);
                if (cobro.esHoy()) {
                    cobrosDeHoy++;
                    totalCobrosDeHoy = totalCobrosDeHoy.add(cobro.valorPendiente());
                }
            }
            PrestamoEnMora atrasado = enMora(prestamo, cliente, hoy);
            if (atrasado != null) {
                mora.insertar(atrasado);
                totalEnMora = totalEnMora.add(atrasado.totalVencido());
            }
        }
        return new Panel(hoy, agenda, mora, cobrosDeHoy, totalCobrosDeHoy, totalEnMora, prestamosActivos, carteraTotal);
    }

    /** La primera cuota sin cubrir que todavía no venció, si vence entre hoy y los próximos días; si no, null. */
    private static CobroAgendado proximoCobro(Prestamo prestamo, Cliente cliente, LocalDate hoy) {
        LocalDate limite = hoy.plusDays(DIAS_DE_AGENDA);
        for (Cuota cuota : prestamo.getCronograma()) {
            if (cuota.estaCubierta() || cuota.getFechaVencimiento().isBefore(hoy)) {
                continue;
            }
            if (cuota.getFechaVencimiento().isAfter(limite)) {
                return null;
            }
            return new CobroAgendado(prestamo.getId(), cliente.getCedula(), cliente.getNombre(), cliente.getTelefono(),
                    cuota.getNumero(), cuota.getFechaVencimiento(), cuota.saldoPendiente(),
                    cuota.getFechaVencimiento().equals(hoy));
        }
        return null;
    }

    /**
     * Si el préstamo tiene cuotas vencidas sin cubrir: cuántas, los días de atraso de la más vieja
     * y el total vencido (lo que falta de esas cuotas más todas las multas pendientes). Si no, null.
     */
    private static PrestamoEnMora enMora(Prestamo prestamo, Cliente cliente, LocalDate hoy) {
        int vencidas = 0;
        int diasDeLaMasVieja = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (Cuota cuota : prestamo.getCronograma()) {
            total = total.add(prestamo.saldoMulta(cuota, hoy));
            if (!cuota.estaCubierta() && cuota.getFechaVencimiento().isBefore(hoy)) {
                if (vencidas == 0) {
                    diasDeLaMasVieja = cuota.diasDeAtraso(hoy);
                }
                vencidas++;
                total = total.add(cuota.saldoPendiente());
            }
        }
        if (vencidas == 0) {
            return null;
        }
        return new PrestamoEnMora(prestamo.getId(), cliente.getCedula(), cliente.getNombre(), cliente.getTelefono(),
                diasDeLaMasVieja, vencidas, total);
    }
}
