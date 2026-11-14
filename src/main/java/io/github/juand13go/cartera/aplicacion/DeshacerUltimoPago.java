package io.github.juand13go.cartera.aplicacion;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.Pago;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/** Caso de uso: el prestamista anula el último pago registrado (por ejemplo, porque lo escribió mal). */
public class DeshacerUltimoPago {

    private final RepositorioPrestamos repositorio;

    public DeshacerUltimoPago(RepositorioPrestamos repositorio) {
        this.repositorio = repositorio;
    }

    /** El dominio saca el pago de la pila (y falla si no hay pagos); luego se borra su fila. */
    public Pago ejecutar(long prestamoId) {
        Prestamo prestamo = cargar(prestamoId);
        Pago deshecho = prestamo.deshacerUltimoPago();
        repositorio.borrarUltimoPago(prestamoId);
        return deshecho;
    }

    private Prestamo cargar(long id) {
        Prestamo prestamo = repositorio.buscarPorId(id);
        if (prestamo == null) {
            throw new IllegalArgumentException("No existe el préstamo " + id);
        }
        return prestamo;
    }
}
