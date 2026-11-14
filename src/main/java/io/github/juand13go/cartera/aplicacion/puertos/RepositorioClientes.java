package io.github.juand13go.cartera.aplicacion.puertos;

import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.modelo.Cliente;

/**
 * Puerto: lo que la aplicación necesita para guardar y leer clientes.
 * La aplicación solo conoce esta interfaz; quién la implementa (JDBC, memoria...) es un detalle.
 */
public interface RepositorioClientes {

    /** Guarda un cliente nuevo. Si ya existe uno con esa cédula, lanza IllegalArgumentException. */
    void guardar(Cliente cliente);

    /** El cliente con esa cédula, o null si no existe. */
    Cliente buscarPorCedula(String cedula);

    /** Todos los clientes, ordenados por nombre. */
    ListaDoble<Cliente> listarTodos();
}
