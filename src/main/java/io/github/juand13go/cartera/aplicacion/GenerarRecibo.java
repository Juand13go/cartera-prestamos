package io.github.juand13go.cartera.aplicacion;

import io.github.juand13go.cartera.aplicacion.puertos.RegistroDePago;
import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.modelo.Pago;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;
import io.github.juand13go.cartera.dominio.modelo.Recibo;

/**
 * Caso de uso: el recibo de un pago. Junta el pago número n del préstamo (lo que calculó el dominio)
 * con el registro número n de la base de datos (número de recibo y fecha-hora).
 */
public class GenerarRecibo {

    private final RepositorioPrestamos repositorio;
    private final DirectorioClientes directorio;

    public GenerarRecibo(RepositorioPrestamos repositorio, DirectorioClientes directorio) {
        this.repositorio = repositorio;
        this.directorio = directorio;
    }

    /** Recibo del pago número {@code numeroDePago} del préstamo (el primero es el 1). */
    public Recibo ejecutar(long prestamoId, int numeroDePago) {
        Prestamo prestamo = repositorio.buscarPorId(prestamoId);
        if (prestamo == null) {
            throw new IllegalArgumentException("No existe el préstamo " + prestamoId);
        }
        ListaDoble<Pago> pagos = delMasViejoAlMasNuevo(prestamo);
        if (numeroDePago < 1 || numeroDePago > pagos.tamano()) {
            throw new IllegalArgumentException("El préstamo " + prestamoId + " no tiene el pago " + numeroDePago);
        }
        Pago pago = pagos.obtener(numeroDePago - 1);
        RegistroDePago registro = repositorio.registrosDePago(prestamoId).obtener(numeroDePago - 1);
        return new Recibo(registro.id(), registro.registradoEn(), prestamoId,
                directorio.buscar(prestamo.getCedulaCliente()), pago);
    }

    /** Recibo del pago más reciente: es al que se va después de registrar un pago. */
    public Recibo ultimo(long prestamoId) {
        Prestamo prestamo = repositorio.buscarPorId(prestamoId);
        if (prestamo == null) {
            throw new IllegalArgumentException("No existe el préstamo " + prestamoId);
        }
        return ejecutar(prestamoId, prestamo.getPagos().tamano());
    }

    /** La pila entrega los pagos del más nuevo al más viejo; agregarAlInicio los deja en el orden contrario. */
    private static ListaDoble<Pago> delMasViejoAlMasNuevo(Prestamo prestamo) {
        ListaDoble<Pago> enOrden = new ListaDoble<>();
        for (Pago pago : prestamo.getPagos()) {
            enOrden.agregarAlInicio(pago);
        }
        return enOrden;
    }
}
