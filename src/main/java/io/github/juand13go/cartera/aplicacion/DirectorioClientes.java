package io.github.juand13go.cartera.aplicacion;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioClientes;
import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.estructuras.TablaHash;
import io.github.juand13go.cartera.dominio.modelo.Cliente;

/**
 * Los clientes por cédula en una TablaHash en memoria, para buscarlos en O(1) sin ir a la base de datos.
 * Se carga una sola vez al arrancar y se mantiene al día cada vez que se crea un cliente.
 * Los métodos son synchronized para que dos peticiones web al mismo tiempo no dañen la tabla.
 */
public class DirectorioClientes {

    private final RepositorioClientes repositorio;
    private final TablaHash<String, Cliente> porCedula;

    public DirectorioClientes(RepositorioClientes repositorio) {
        this.repositorio = repositorio;
        this.porCedula = new TablaHash<>();
        for (Cliente cliente : repositorio.listarTodos()) {
            porCedula.guardar(cliente.getCedula(), cliente);
        }
    }

    /** El cliente con esa cédula, o null si no existe. O(1) en promedio: es un buscar en la tabla hash. */
    public synchronized Cliente buscar(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            return null;
        }
        return porCedula.buscar(cedula.trim());
    }

    /** Agrega a la tabla un cliente que ya se guardó en la base de datos. O(1) en promedio. */
    public synchronized void agregar(Cliente cliente) {
        porCedula.guardar(cliente.getCedula(), cliente);
    }

    /** Cuántos clientes hay. O(1). */
    public synchronized int cantidad() {
        return porCedula.tamano();
    }

    /**
     * Todos los clientes ordenados por nombre, para mostrarlos en una lista.
     * Se piden al repositorio porque la tabla hash no guarda ningún orden.
     */
    public ListaDoble<Cliente> listarPorNombre() {
        return repositorio.listarTodos();
    }
}
