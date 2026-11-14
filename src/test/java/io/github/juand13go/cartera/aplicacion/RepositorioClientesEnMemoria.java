package io.github.juand13go.cartera.aplicacion;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioClientes;
import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.modelo.Cliente;

/** Repositorio falso para probar los casos de uso sin base de datos. No ordena: no hace falta en las pruebas. */
class RepositorioClientesEnMemoria implements RepositorioClientes {

    private final ListaDoble<Cliente> clientes = new ListaDoble<>();
    int vecesListado = 0;

    @Override
    public void guardar(Cliente cliente) {
        if (buscarPorCedula(cliente.getCedula()) != null) {
            throw new IllegalArgumentException("Ya existe un cliente con la cédula " + cliente.getCedula());
        }
        clientes.agregarAlFinal(cliente);
    }

    @Override
    public Cliente buscarPorCedula(String cedula) {
        for (Cliente cliente : clientes) {
            if (cliente.getCedula().equals(cedula)) {
                return cliente;
            }
        }
        return null;
    }

    @Override
    public ListaDoble<Cliente> listarTodos() {
        vecesListado++;
        return clientes;
    }
}
