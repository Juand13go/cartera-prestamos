package io.github.juand13go.cartera.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.modelo.Cliente;

class IniciarSesionTest {

    private IniciarSesion iniciarSesion;

    @BeforeEach
    void preparar() {
        RepositorioClientesEnMemoria clientes = new RepositorioClientesEnMemoria();
        clientes.guardar(new Cliente("1000000001", "Laura Gómez", "3000000001", null, null,
                new BigDecimal("5000000"), null, "cifrado:1234"));
        // El CifradorFalso "cifra" cartera2026 como cifrado:cartera2026.
        iniciarSesion = new IniciarSesion(new DirectorioClientes(clientes), new CifradorFalso(),
                "prestamista", "cifrado:cartera2026");
    }

    @Test
    void elPrestamistaEntraConSuUsuarioYClave() {
        UsuarioSesion usuario = iniciarSesion.prestamista("prestamista", "cartera2026");

        assertTrue(usuario.esPrestamista());
        assertNull(usuario.cedula());
        assertEquals("prestamista", usuario.nombre());
    }

    @Test
    void elPrestamistaNoEntraConUnaClaveOUsuarioEquivocados() {
        IllegalArgumentException claveMala = assertThrows(IllegalArgumentException.class,
                () -> iniciarSesion.prestamista("prestamista", "otra"));
        IllegalArgumentException usuarioMalo = assertThrows(IllegalArgumentException.class,
                () -> iniciarSesion.prestamista("admin", "cartera2026"));

        // El mismo mensaje en los dos casos: no se revela qué falló.
        assertEquals(claveMala.getMessage(), usuarioMalo.getMessage());
        assertThrows(IllegalArgumentException.class, () -> iniciarSesion.prestamista(null, null));
    }

    @Test
    void elClienteEntraConSuCedulaYPin() {
        UsuarioSesion usuario = iniciarSesion.cliente("1000000001", "1234");

        assertTrue(usuario.esCliente());
        assertEquals("1000000001", usuario.cedula());
        assertEquals("Laura Gómez", usuario.nombre());
    }

    @Test
    void elClienteNoEntraConUnPinMaloOUnaCedulaQueNoExiste() {
        IllegalArgumentException pinMalo = assertThrows(IllegalArgumentException.class,
                () -> iniciarSesion.cliente("1000000001", "9999"));
        IllegalArgumentException noExiste = assertThrows(IllegalArgumentException.class,
                () -> iniciarSesion.cliente("1000000099", "1234"));

        assertEquals(IniciarSesion.CREDENCIALES_INCORRECTAS, pinMalo.getMessage());
        assertEquals(pinMalo.getMessage(), noExiste.getMessage());
        assertThrows(IllegalArgumentException.class, () -> iniciarSesion.cliente("1000000001", null));
    }
}
