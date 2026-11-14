package io.github.juand13go.cartera.aplicacion.puertos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.modelo.Contrato;
import io.github.juand13go.cartera.dominio.modelo.Pago;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/**
 * Puerto: lo que la aplicación necesita para guardar y leer préstamos.
 * Solo se guarda lo que pasó (contrato, pagos y condonaciones). Al leer, el préstamo
 * se reconstruye: se crea con su contrato, se restauran las condonaciones y se repiten los pagos en orden.
 */
public interface RepositorioPrestamos {

    /** Guarda un préstamo nuevo con su contrato y devuelve el id que le asignó la base de datos. */
    long crear(String cedulaCliente, Contrato contrato);

    /** El préstamo reconstruido, o null si no existe. */
    Prestamo buscarPorId(long id);

    /** Los préstamos de un cliente, del más reciente al más antiguo. */
    ListaDoble<Prestamo> listarPorCliente(String cedulaCliente);

    /** Todos los préstamos, del más reciente al más antiguo. */
    ListaDoble<Prestamo> listarTodos();

    /** Agrega un pago al final del historial del préstamo. */
    void guardarPago(long prestamoId, Pago pago, LocalDateTime registradoEn);

    /** Borra el pago más reciente del préstamo (deshacer). */
    void borrarUltimoPago(long prestamoId);

    /** Anota que la multa de una cuota quedó condonada en ese valor. */
    void guardarCondonacion(long prestamoId, int numeroCuota, BigDecimal fijadaEn);
}
