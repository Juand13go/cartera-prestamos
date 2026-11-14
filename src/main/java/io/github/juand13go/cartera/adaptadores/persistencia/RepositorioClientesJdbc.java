package io.github.juand13go.cartera.adaptadores.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioClientes;
import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.modelo.Cliente;

/** Adaptador: guarda y lee clientes con JdbcTemplate y SQL escrito a mano. */
@Repository
public class RepositorioClientesJdbc implements RepositorioClientes {

    private static final String COLUMNAS =
            "cedula, nombre, telefono, direccion, referencia, cupo_maximo, notas, pin_cifrado";

    private final JdbcTemplate jdbc;

    public RepositorioClientesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(Cliente cliente) {
        try {
            jdbc.update("INSERT INTO cliente (" + COLUMNAS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    cliente.getCedula(), cliente.getNombre(), cliente.getTelefono(), cliente.getDireccion(),
                    cliente.getReferencia(), cliente.getCupoMaximo(), cliente.getNotas(), cliente.getPinCifrado());
        } catch (DuplicateKeyException e) {
            // La aplicación no debe conocer excepciones de Spring: se traduce a una del lenguaje.
            throw new IllegalArgumentException("Ya existe un cliente con la cédula " + cliente.getCedula());
        }
    }

    @Override
    public Cliente buscarPorCedula(String cedula) {
        ListaDoble<Cliente> encontrados = new ListaDoble<>();
        jdbc.query("SELECT " + COLUMNAS + " FROM cliente WHERE cedula = ?",
                fila -> { encontrados.agregarAlFinal(aCliente(fila)); }, cedula);
        return encontrados.estaVacia() ? null : encontrados.primero();
    }

    /** Cada fila del resultado se agrega directamente a nuestra lista, sin pasar por colecciones de java.util. */
    @Override
    public ListaDoble<Cliente> listarTodos() {
        ListaDoble<Cliente> clientes = new ListaDoble<>();
        jdbc.query("SELECT " + COLUMNAS + " FROM cliente ORDER BY nombre",
                fila -> { clientes.agregarAlFinal(aCliente(fila)); });
        return clientes;
    }

    /** Convierte la fila actual del resultado en un Cliente. */
    private static Cliente aCliente(ResultSet fila) throws SQLException {
        return new Cliente(
                fila.getString("cedula"),
                fila.getString("nombre"),
                fila.getString("telefono"),
                fila.getString("direccion"),
                fila.getString("referencia"),
                fila.getBigDecimal("cupo_maximo"),
                fila.getString("notas"),
                fila.getString("pin_cifrado"));
    }
}
