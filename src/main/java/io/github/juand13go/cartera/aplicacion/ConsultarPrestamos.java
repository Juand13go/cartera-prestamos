package io.github.juand13go.cartera.aplicacion;

import java.time.Clock;
import java.time.LocalDate;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/**
 * Caso de uso: ver préstamos tal como están hoy.
 * Cada préstamo leído se actualiza a la fecha del reloj, para que aparezcan las cuotas que ya nacieron.
 */
public class ConsultarPrestamos {

    private final RepositorioPrestamos repositorio;
    private final Clock reloj;

    public ConsultarPrestamos(RepositorioPrestamos repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /** La fecha de hoy según el reloj del sistema (real o simulada). */
    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /** El préstamo actualizado a hoy. Si no existe, lanza IllegalArgumentException. */
    public Prestamo porId(long id) {
        Prestamo prestamo = repositorio.buscarPorId(id);
        if (prestamo == null) {
            throw new IllegalArgumentException("No existe el préstamo " + id);
        }
        prestamo.actualizarCronograma(hoy());
        return prestamo;
    }

    /** Los préstamos de un cliente, actualizados a hoy, del más reciente al más antiguo. */
    public ListaDoble<Prestamo> deCliente(String cedula) {
        return actualizados(repositorio.listarPorCliente(cedula));
    }

    /** Todos los préstamos, actualizados a hoy. */
    public ListaDoble<Prestamo> todos() {
        return actualizados(repositorio.listarTodos());
    }

    private ListaDoble<Prestamo> actualizados(ListaDoble<Prestamo> prestamos) {
        LocalDate hoy = hoy();
        for (Prestamo prestamo : prestamos) {
            prestamo.actualizarCronograma(hoy);
        }
        return prestamos;
    }
}
