package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.Cliente;
import io.github.juand13go.cartera.dominio.modelo.Contrato;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/** Caso de uso: el prestamista crea un préstamo para un cliente con un contrato ya firmado. */
public class CrearPrestamo {

    private final RepositorioPrestamos repositorio;
    private final DirectorioClientes directorio;
    private final BigDecimal montoMinimo;

    public CrearPrestamo(RepositorioPrestamos repositorio, DirectorioClientes directorio, BigDecimal montoMinimo) {
        this.repositorio = repositorio;
        this.directorio = directorio;
        this.montoMinimo = montoMinimo;
    }

    /**
     * Valida el cliente, el monto mínimo y el cupo; guarda el contrato y devuelve el préstamo con su id.
     *
     * @param confirmaSobreCupo true cuando el prestamista ya vio la advertencia de cupo y decidió seguir
     * @throws CupoExcedidoException si se supera el cupo y no se ha confirmado
     */
    public Prestamo ejecutar(String cedulaCliente, Contrato contrato, boolean confirmaSobreCupo) {
        Cliente cliente = directorio.buscar(cedulaCliente);
        if (cliente == null) {
            throw new IllegalArgumentException("No existe un cliente con la cédula " + cedulaCliente);
        }
        BigDecimal capital = contrato.getCapital();
        Dinero.validarSinCentavos(capital, "El capital");
        if (capital.compareTo(montoMinimo) < 0) {
            throw new IllegalArgumentException("El capital mínimo es " + montoMinimo);
        }
        BigDecimal deudaVigente = deudaVigente(cliente.getCedula());
        if (deudaVigente.add(capital).compareTo(cliente.getCupoMaximo()) > 0 && !confirmaSobreCupo) {
            throw new CupoExcedidoException(cliente.getCupoMaximo(), deudaVigente, capital);
        }
        long id = repositorio.crear(cliente.getCedula(), contrato);
        return new Prestamo(id, cliente.getCedula(), contrato);
    }

    /** Capital que el cliente todavía debe en sus préstamos activos (sin intereses ni multas). */
    private BigDecimal deudaVigente(String cedula) {
        BigDecimal total = BigDecimal.ZERO;
        for (Prestamo prestamo : repositorio.listarPorCliente(cedula)) {
            if (!prestamo.estaCerrado()) {
                total = total.add(prestamo.capitalAdeudado());
            }
        }
        return total;
    }
}
