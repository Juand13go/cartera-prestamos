package io.github.juand13go.cartera.aplicacion;

import io.github.juand13go.cartera.aplicacion.puertos.CifradorPin;
import io.github.juand13go.cartera.aplicacion.puertos.RepositorioClientes;
import io.github.juand13go.cartera.dominio.modelo.Cliente;

/** Caso de uso: el prestamista registra un cliente nuevo con su PIN. */
public class CrearCliente {

    private final RepositorioClientes repositorio;
    private final DirectorioClientes directorio;
    private final CifradorPin cifrador;

    public CrearCliente(RepositorioClientes repositorio, DirectorioClientes directorio, CifradorPin cifrador) {
        this.repositorio = repositorio;
        this.directorio = directorio;
        this.cifrador = cifrador;
    }

    /**
     * Valida, cifra el PIN, guarda en la base de datos y agrega a la tabla hash.
     * El PIN real no se guarda en ninguna parte.
     */
    public Cliente ejecutar(DatosNuevoCliente datos) {
        if (datos.pin() == null || !datos.pin().matches("\\d{4}")) {
            throw new IllegalArgumentException("El PIN debe tener 4 dígitos");
        }
        if (directorio.buscar(datos.cedula()) != null) {
            throw new IllegalArgumentException("Ya existe un cliente con la cédula " + datos.cedula());
        }
        Cliente cliente = new Cliente(datos.cedula(), datos.nombre(), datos.telefono(), datos.direccion(),
                datos.referencia(), datos.cupoMaximo(), datos.notas(), cifrador.cifrar(datos.pin()));
        repositorio.guardar(cliente);
        directorio.agregar(cliente);
        return cliente;
    }
}
