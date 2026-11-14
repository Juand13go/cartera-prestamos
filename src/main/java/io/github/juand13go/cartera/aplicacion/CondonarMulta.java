package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/** Caso de uso: el prestamista anula lo que falta de la multa de una cuota. */
public class CondonarMulta {

    private final RepositorioPrestamos repositorio;
    private final Clock reloj;

    public CondonarMulta(RepositorioPrestamos repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /** El dominio fija la multa en lo ya pagado y devuelve ese valor, que es lo que se guarda. */
    public BigDecimal ejecutar(long prestamoId, int numeroCuota) {
        Prestamo prestamo = cargar(prestamoId);
        BigDecimal fijadaEn = prestamo.condonarMulta(numeroCuota, LocalDate.now(reloj));
        repositorio.guardarCondonacion(prestamoId, numeroCuota, fijadaEn);
        return fijadaEn;
    }

    private Prestamo cargar(long id) {
        Prestamo prestamo = repositorio.buscarPorId(id);
        if (prestamo == null) {
            throw new IllegalArgumentException("No existe el préstamo " + id);
        }
        return prestamo;
    }
}
