package io.github.juand13go.cartera.adaptadores.persistencia;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.modelo.BaseInteres;
import io.github.juand13go.cartera.dominio.modelo.Contrato;
import io.github.juand13go.cartera.dominio.modelo.Pago;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;
import io.github.juand13go.cartera.dominio.politicas.AlVencerPlazo;
import io.github.juand13go.cartera.dominio.politicas.Calendario;
import io.github.juand13go.cartera.dominio.politicas.InteresCancelacion;
import io.github.juand13go.cartera.dominio.politicas.OrdenImputacion;

/**
 * Adaptador: guarda contratos, pagos y condonaciones con JdbcTemplate y SQL escrito a mano,
 * y reconstruye cada préstamo repitiendo sus pagos en orden.
 */
@Repository
public class RepositorioPrestamosJdbc implements RepositorioPrestamos {

    private static final String COLUMNAS_PRESTAMO = """
            id, cedula_cliente, capital, tasa_por_periodo, base_interes, fecha_desembolso, frecuencia, dia_cobro,
            calendario, politica_mora, valor_mora, dias_gracia, orden_imputacion, al_vencer_plazo, interes_cancelacion""";

    private final JdbcTemplate jdbc;

    public RepositorioPrestamosJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long crear(String cedulaCliente, Contrato contrato) {
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            PreparedStatement sql = conexion.prepareStatement("""
                    INSERT INTO prestamo (cedula_cliente, capital, tasa_por_periodo, base_interes, fecha_desembolso,
                        frecuencia, dia_cobro, calendario, politica_mora, valor_mora, dias_gracia,
                        orden_imputacion, al_vencer_plazo, interes_cancelacion)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, new String[] {"id"});
            sql.setString(1, cedulaCliente);
            sql.setBigDecimal(2, contrato.getCapital());
            sql.setBigDecimal(3, contrato.getTasaPorPeriodo());
            sql.setString(4, contrato.getBaseInteres().name());
            sql.setObject(5, contrato.getFechaDesembolso());
            sql.setString(6, ConversorContrato.tipoFrecuencia(contrato.getFrecuencia()));
            sql.setObject(7, ConversorContrato.diaDeCobro(contrato.getFrecuencia()));
            sql.setString(8, contrato.getCalendario().name());
            sql.setString(9, ConversorContrato.tipoMora(contrato.getPoliticaMora()));
            sql.setBigDecimal(10, ConversorContrato.valorMora(contrato.getPoliticaMora()));
            sql.setInt(11, contrato.getDiasDeGracia());
            sql.setString(12, contrato.getOrdenImputacion().name());
            sql.setString(13, contrato.getAlVencerPlazo().name());
            sql.setString(14, contrato.getInteresCancelacion().name());
            return sql;
        }, llave);
        return llave.getKey().longValue();
    }

    @Override
    public Prestamo buscarPorId(long id) {
        ListaDoble<Prestamo> encontrados = leerPrestamos("WHERE id = ?", id);
        return encontrados.estaVacia() ? null : encontrados.primero();
    }

    @Override
    public ListaDoble<Prestamo> listarPorCliente(String cedulaCliente) {
        return leerPrestamos("WHERE cedula_cliente = ? ORDER BY id DESC", cedulaCliente);
    }

    @Override
    public ListaDoble<Prestamo> listarTodos() {
        return leerPrestamos("ORDER BY id DESC");
    }

    @Override
    public void guardarPago(long prestamoId, Pago pago, LocalDateTime registradoEn) {
        jdbc.update("INSERT INTO pago (prestamo_id, fecha, monto, cancelacion, registrado_en) VALUES (?, ?, ?, ?, ?)",
                prestamoId, pago.getFecha(), pago.getMonto(), pago.isCancelacion(), Timestamp.valueOf(registradoEn));
    }

    @Override
    public void borrarUltimoPago(long prestamoId) {
        jdbc.update("DELETE FROM pago WHERE id = (SELECT MAX(id) FROM pago WHERE prestamo_id = ?)", prestamoId);
    }

    @Override
    public void guardarCondonacion(long prestamoId, int numeroCuota, BigDecimal fijadaEn) {
        jdbc.update("INSERT INTO condonacion (prestamo_id, numero_cuota, multa_fijada_en) VALUES (?, ?, ?)",
                prestamoId, numeroCuota, fijadaEn);
    }

    /**
     * Lee las filas de préstamos que cumplan el filtro y reconstruye cada uno.
     * Primero se leen todas las filas y después se reconstruye, para no abrir consultas anidadas.
     */
    private ListaDoble<Prestamo> leerPrestamos(String filtro, Object... parametros) {
        ListaDoble<Prestamo> recienCreados = new ListaDoble<>();
        jdbc.query("SELECT " + COLUMNAS_PRESTAMO + " FROM prestamo " + filtro,
                fila -> { recienCreados.agregarAlFinal(aPrestamoSinPagos(fila)); }, parametros);

        for (Prestamo prestamo : recienCreados) {
            restaurarCondonaciones(prestamo);
            repetirPagos(prestamo);
        }
        return recienCreados;
    }

    /** Crea el préstamo como el día del desembolso, con su contrato leído de la fila. */
    private static Prestamo aPrestamoSinPagos(ResultSet fila) throws SQLException {
        Contrato contrato = Contrato.borrador(
                        fila.getBigDecimal("capital"),
                        fila.getBigDecimal("tasa_por_periodo"),
                        fila.getObject("fecha_desembolso", LocalDate.class))
                .base(BaseInteres.valueOf(fila.getString("base_interes")))
                .frecuencia(ConversorContrato.frecuencia(fila.getString("frecuencia"),
                        fila.getObject("dia_cobro", Integer.class)))
                .calendario(Calendario.valueOf(fila.getString("calendario")))
                .mora(ConversorContrato.politicaMora(fila.getString("politica_mora"), fila.getBigDecimal("valor_mora")))
                .diasDeGracia(fila.getInt("dias_gracia"))
                .ordenImputacion(OrdenImputacion.valueOf(fila.getString("orden_imputacion")))
                .alVencerPlazo(AlVencerPlazo.valueOf(fila.getString("al_vencer_plazo")))
                .interesCancelacion(InteresCancelacion.valueOf(fila.getString("interes_cancelacion")))
                .firmar();
        return new Prestamo(fila.getLong("id"), fila.getString("cedula_cliente"), contrato);
    }

    private void restaurarCondonaciones(Prestamo prestamo) {
        jdbc.query("SELECT numero_cuota, multa_fijada_en FROM condonacion WHERE prestamo_id = ?",
                fila -> {
                    prestamo.restaurarCondonacion(fila.getInt("numero_cuota"), fila.getBigDecimal("multa_fijada_en"));
                }, prestamo.getId());
    }

    /** Repite los pagos en el orden en que ocurrieron (por id), igual que cuando se registraron. */
    private void repetirPagos(Prestamo prestamo) {
        jdbc.query("SELECT fecha, monto, cancelacion FROM pago WHERE prestamo_id = ? ORDER BY id",
                fila -> {
                    LocalDate fecha = fila.getObject("fecha", LocalDate.class);
                    if (fila.getBoolean("cancelacion")) {
                        prestamo.cancelar(fecha);
                    } else {
                        prestamo.registrarPago(fecha, fila.getBigDecimal("monto"));
                    }
                }, prestamo.getId());
    }
}
