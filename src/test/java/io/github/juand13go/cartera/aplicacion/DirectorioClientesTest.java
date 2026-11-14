package io.github.juand13go.cartera.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.modelo.Cliente;

class DirectorioClientesTest {

    private static Cliente cliente(String cedula) {
        return new Cliente(cedula, "Cliente " + cedula, "3001234567", null, null,
                new BigDecimal("1000000"), null, "cifrado:1234");
    }

    @Test
    void alCrearseCargaTodosLosClientesDelRepositorioUnaSolaVez() {
        RepositorioClientesEnMemoria repositorio = new RepositorioClientesEnMemoria();
        repositorio.guardar(cliente("1001001"));
        repositorio.guardar(cliente("1001002"));

        DirectorioClientes directorio = new DirectorioClientes(repositorio);

        assertEquals(2, directorio.cantidad());
        assertEquals(1, repositorio.vecesListado);
    }

    @Test
    void buscarUsaLaTablaHashSinVolverAlRepositorio() {
        RepositorioClientesEnMemoria repositorio = new RepositorioClientesEnMemoria();
        Cliente ana = cliente("1001001");
        repositorio.guardar(ana);
        DirectorioClientes directorio = new DirectorioClientes(repositorio);

        assertSame(ana, directorio.buscar("1001001"));
        assertSame(ana, directorio.buscar(" 1001001 "));
        assertEquals(1, repositorio.vecesListado);
    }

    @Test
    void buscarUnaCedulaQueNoExisteOVaciaDevuelveNull() {
        DirectorioClientes directorio = new DirectorioClientes(new RepositorioClientesEnMemoria());

        assertNull(directorio.buscar("9999999"));
        assertNull(directorio.buscar(""));
        assertNull(directorio.buscar(null));
    }

    @Test
    void unClienteAgregadoSeEncuentraDeInmediato() {
        DirectorioClientes directorio = new DirectorioClientes(new RepositorioClientesEnMemoria());
        Cliente nuevo = cliente("1001003");

        directorio.agregar(nuevo);

        assertSame(nuevo, directorio.buscar("1001003"));
        assertEquals(1, directorio.cantidad());
    }
}
