package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.estructuras.TablaHash;
import io.github.juand13go.cartera.dominio.modelo.Contrato;
import io.github.juand13go.cartera.dominio.modelo.Pago;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/**
 * Repositorio falso que se comporta como el real: guarda solo contrato, pagos y condonaciones,
 * y en cada lectura reconstruye un préstamo nuevo. Así, si un caso de uso olvida guardar algo, la prueba lo nota.
 */
class RepositorioPrestamosEnMemoria implements RepositorioPrestamos {

    /** Lo que se guardaría en las tablas para un préstamo. */
    private static class Registro {
        final long id;
        final String cedula;
        final Contrato contrato;
        final ListaDoble<Pago> pagos = new ListaDoble<>();
        final ListaDoble<Integer> cuotasCondonadas = new ListaDoble<>();
        final ListaDoble<BigDecimal> valoresCondonados = new ListaDoble<>();

        Registro(long id, String cedula, Contrato contrato) {
            this.id = id;
            this.cedula = cedula;
            this.contrato = contrato;
        }
    }

    private final TablaHash<Long, Registro> registros = new TablaHash<>();
    private final ListaDoble<Long> ids = new ListaDoble<>();
    private long siguienteId = 1;
    int pagosGuardados = 0;
    LocalDateTime ultimoRegistradoEn;

    @Override
    public long crear(String cedulaCliente, Contrato contrato) {
        long id = siguienteId++;
        registros.guardar(id, new Registro(id, cedulaCliente, contrato));
        ids.agregarAlInicio(id);
        return id;
    }

    @Override
    public Prestamo buscarPorId(long id) {
        Registro registro = registros.buscar(id);
        return registro == null ? null : reconstruir(registro);
    }

    @Override
    public ListaDoble<Prestamo> listarPorCliente(String cedulaCliente) {
        ListaDoble<Prestamo> resultado = new ListaDoble<>();
        for (Long id : ids) {
            Registro registro = registros.buscar(id);
            if (registro.cedula.equals(cedulaCliente)) {
                resultado.agregarAlFinal(reconstruir(registro));
            }
        }
        return resultado;
    }

    @Override
    public ListaDoble<Prestamo> listarTodos() {
        ListaDoble<Prestamo> resultado = new ListaDoble<>();
        for (Long id : ids) {
            resultado.agregarAlFinal(reconstruir(registros.buscar(id)));
        }
        return resultado;
    }

    @Override
    public void guardarPago(long prestamoId, Pago pago, LocalDateTime registradoEn) {
        registros.buscar(prestamoId).pagos.agregarAlFinal(pago);
        pagosGuardados++;
        ultimoRegistradoEn = registradoEn;
    }

    @Override
    public void borrarUltimoPago(long prestamoId) {
        registros.buscar(prestamoId).pagos.eliminarUltimo();
    }

    @Override
    public void guardarCondonacion(long prestamoId, int numeroCuota, BigDecimal fijadaEn) {
        Registro registro = registros.buscar(prestamoId);
        registro.cuotasCondonadas.agregarAlFinal(numeroCuota);
        registro.valoresCondonados.agregarAlFinal(fijadaEn);
    }

    /** Igual que el repositorio JDBC: préstamo nuevo, condonaciones, y los pagos repetidos en orden. */
    private static Prestamo reconstruir(Registro registro) {
        Prestamo prestamo = new Prestamo(registro.id, registro.cedula, registro.contrato);
        for (int i = 0; i < registro.cuotasCondonadas.tamano(); i++) {
            prestamo.restaurarCondonacion(registro.cuotasCondonadas.obtener(i), registro.valoresCondonados.obtener(i));
        }
        for (Pago pago : registro.pagos) {
            LocalDate fecha = pago.getFecha();
            if (pago.isCancelacion()) {
                prestamo.cancelar(fecha);
            } else {
                prestamo.registrarPago(fecha, pago.getMonto());
            }
        }
        return prestamo;
    }
}
