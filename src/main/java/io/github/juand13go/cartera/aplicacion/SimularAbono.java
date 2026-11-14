package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.BaseInteres;
import io.github.juand13go.cartera.dominio.modelo.Pago;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/**
 * Caso de uso: "si abono $X hoy, ¿cómo queda mi préstamo?".
 * Aplica el abono a una copia del préstamo recién reconstruida desde la base de datos y NO guarda nada:
 * por eso un cliente (que solo puede leer) puede usarlo.
 */
public class SimularAbono {

    private final RepositorioPrestamos repositorio;
    private final Clock reloj;

    public SimularAbono(RepositorioPrestamos repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    public ResultadoSimulacion ejecutar(long prestamoId, BigDecimal abono) {
        Dinero.validarSinCentavos(abono, "El abono");
        Prestamo copia = repositorio.buscarPorId(prestamoId);
        if (copia == null) {
            throw new IllegalArgumentException("No existe el préstamo " + prestamoId);
        }
        LocalDate hoy = LocalDate.now(reloj);
        copia.actualizarCronograma(hoy);
        BigDecimal capitalAntes = copia.capitalAdeudado();
        BigDecimal interesAntes = copia.interesProximaCuota();

        // El dominio reparte y valida igual que un pago real; la copia se descarta al terminar.
        Pago pago = copia.registrarPago(hoy, abono);

        return new ResultadoSimulacion(pago, capitalAntes, copia.capitalAdeudado(), interesAntes,
                copia.interesProximaCuota(), copia.getContrato().getBaseInteres() == BaseInteres.CAPITAL_VIGENTE);
    }
}
