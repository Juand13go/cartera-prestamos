package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.Pago;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/** Caso de uso: el cliente entrega dinero hoy y el prestamista lo registra. */
public class RegistrarPago {

    private final RepositorioPrestamos repositorio;
    private final Clock reloj;

    public RegistrarPago(RepositorioPrestamos repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /**
     * Carga el préstamo, el dominio reparte y valida el pago, y se guarda solo la fila del pago.
     * Si el dominio rechaza el pago, no se guarda nada.
     */
    public Pago ejecutar(long prestamoId, BigDecimal monto) {
        Dinero.validarSinCentavos(monto, "El pago");
        Prestamo prestamo = cargar(prestamoId);
        Pago pago = prestamo.registrarPago(LocalDate.now(reloj), monto);
        repositorio.guardarPago(prestamoId, pago, LocalDateTime.now(reloj));
        return pago;
    }

    private Prestamo cargar(long id) {
        Prestamo prestamo = repositorio.buscarPorId(id);
        if (prestamo == null) {
            throw new IllegalArgumentException("No existe el préstamo " + id);
        }
        return prestamo;
    }
}
