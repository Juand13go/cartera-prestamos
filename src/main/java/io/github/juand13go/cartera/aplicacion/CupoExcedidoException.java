package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;

/**
 * El préstamo nuevo haría que el cliente supere su cupo. No es un error del sistema:
 * la pantalla debe mostrar estos valores y pedir confirmación explícita al prestamista.
 */
public class CupoExcedidoException extends RuntimeException {

    private final BigDecimal cupo;
    private final BigDecimal deudaVigente;
    private final BigDecimal montoSolicitado;

    public CupoExcedidoException(BigDecimal cupo, BigDecimal deudaVigente, BigDecimal montoSolicitado) {
        super("El cliente debe " + deudaVigente + " y pide " + montoSolicitado + ": supera su cupo de " + cupo);
        this.cupo = cupo;
        this.deudaVigente = deudaVigente;
        this.montoSolicitado = montoSolicitado;
    }

    public BigDecimal getCupo() {
        return cupo;
    }

    public BigDecimal getDeudaVigente() {
        return deudaVigente;
    }

    public BigDecimal getMontoSolicitado() {
        return montoSolicitado;
    }
}
