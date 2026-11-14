package io.github.juand13go.cartera.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.Pago;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/** Caso de uso: el cliente paga hoy todo lo que debe y el préstamo se cierra. */
public class CancelarPrestamo {

    private final RepositorioPrestamos repositorio;
    private final Clock reloj;

    public CancelarPrestamo(RepositorioPrestamos repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /** El dominio calcula el valor según el contrato y lo paga; se guarda como pago marcado de cancelación. */
    public Pago ejecutar(long prestamoId) {
        Prestamo prestamo = cargar(prestamoId);
        Pago pago = prestamo.cancelar(LocalDate.now(reloj));
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
