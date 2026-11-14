package io.github.juand13go.cartera.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.modelo.Cliente;

class CrearClienteTest {

    private RepositorioClientesEnMemoria repositorio;
    private DirectorioClientes directorio;
    private CrearCliente crearCliente;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioClientesEnMemoria();
        directorio = new DirectorioClientes(repositorio);
        crearCliente = new CrearCliente(repositorio, directorio, new CifradorFalso());
    }

    private static DatosNuevoCliente datos(String cedula, String pin) {
        return new DatosNuevoCliente(cedula, "Ana Pérez", "3001234567", "Calle 1", "Vecina",
                new BigDecimal("5000000"), "Notas", pin);
    }

    @Test
    void guardaElClienteConElPinCifradoYLoAgregaALaTabla() {
        Cliente creado = crearCliente.ejecutar(datos("1020304050", "1234"));

        assertEquals("cifrado:1234", creado.getPinCifrado());
        assertNotNull(repositorio.buscarPorCedula("1020304050"));
        assertEquals(creado, directorio.buscar("1020304050"));
    }

    @Test
    void noPermiteDosClientesConLaMismaCedula() {
        crearCliente.ejecutar(datos("1020304050", "1234"));

        assertThrows(IllegalArgumentException.class, () -> crearCliente.ejecutar(datos("1020304050", "5678")));
    }

    @Test
    void elPinDebeTener4Digitos() {
        assertThrows(IllegalArgumentException.class, () -> crearCliente.ejecutar(datos("1020304050", "123")));
        assertThrows(IllegalArgumentException.class, () -> crearCliente.ejecutar(datos("1020304050", "12345")));
        assertThrows(IllegalArgumentException.class, () -> crearCliente.ejecutar(datos("1020304050", "12a4")));
        assertThrows(IllegalArgumentException.class, () -> crearCliente.ejecutar(datos("1020304050", null)));
    }

    @Test
    void siLosDatosSonInvalidosNoSeGuardaNada() {
        DatosNuevoCliente telefonoMalo = new DatosNuevoCliente("1020304050", "Ana", "123", null, null,
                new BigDecimal("5000000"), null, "1234");

        assertThrows(IllegalArgumentException.class, () -> crearCliente.ejecutar(telefonoMalo));
        assertEquals(0, directorio.cantidad());
        assertEquals(null, repositorio.buscarPorCedula("1020304050"));
    }
}
