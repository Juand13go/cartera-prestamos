package io.github.juand13go.cartera.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.modelo.Cliente;

/** Un solo método, entrar(identificador, clave): el rol lo deduce el sistema, no lo escoge el usuario. */
class IniciarSesionTest {

    private DirectorioClientes directorio;
    private IniciarSesion iniciarSesion;

    @BeforeEach
    void preparar() {
        RepositorioClientesEnMemoria clientes = new RepositorioClientesEnMemoria();
        clientes.guardar(new Cliente("1000000001", "Laura Gómez", "3000000001", null, null,
                new BigDecimal("5000000"), null, "cifrado:1234"));
        directorio = new DirectorioClientes(clientes);
        // El CifradorFalso "cifra" cartera2026 como cifrado:cartera2026.
        iniciarSesion = new IniciarSesion(directorio, new CifradorFalso(), "prestamista", "cifrado:cartera2026");
    }

    @Test
    void elPrestamistaEntraConSuUsuarioYQuedaConRolPrestamista() {
        UsuarioSesion usuario = iniciarSesion.entrar("prestamista", "cartera2026");

        assertTrue(usuario.esPrestamista());
        assertNull(usuario.cedula());
        assertEquals("prestamista", usuario.nombre());
    }

    @Test
    void unClienteEntraConSuCedulaYSuPinYQuedaConRolCliente() {
        UsuarioSesion usuario = iniciarSesion.entrar(" 1000000001 ", "1234");

        assertTrue(usuario.esCliente());
        assertEquals("1000000001", usuario.cedula());
        assertEquals("Laura Gómez", usuario.nombre());
    }

    @Test
    void unaClaveIncorrectaDaElMensajeGenericoParaPrestamistaYCliente() {
        IllegalArgumentException prestamista = assertThrows(IllegalArgumentException.class,
                () -> iniciarSesion.entrar("prestamista", "otra"));
        IllegalArgumentException cliente = assertThrows(IllegalArgumentException.class,
                () -> iniciarSesion.entrar("1000000001", "9999"));

        assertEquals("Usuario o clave incorrectos", prestamista.getMessage());
        assertEquals("Usuario o clave incorrectos", cliente.getMessage());
    }

    @Test
    void unaCuentaQueNoExisteDaElMismoMensaje() {
        IllegalArgumentException cedula = assertThrows(IllegalArgumentException.class,
                () -> iniciarSesion.entrar("1000000099", "1234"));
        IllegalArgumentException usuario = assertThrows(IllegalArgumentException.class,
                () -> iniciarSesion.entrar("admin", "cartera2026"));

        assertEquals(IniciarSesion.CREDENCIALES_INCORRECTAS, cedula.getMessage());
        assertEquals(IniciarSesion.CREDENCIALES_INCORRECTAS, usuario.getMessage());
        assertThrows(IllegalArgumentException.class, () -> iniciarSesion.entrar(null, null));
    }

    @Test
    void laContrasenaDelPrestamistaNoSirveComoPinDeUnCliente() {
        assertThrows(IllegalArgumentException.class, () -> iniciarSesion.entrar("1000000001", "cartera2026"));
    }

    @Test
    void unUsuarioDePrestamistaNumericoSeRechazaAlConstruir() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new IniciarSesion(directorio, new CifradorFalso(), "1000000001", "cifrado:cartera2026"));

        assertTrue(error.getMessage().contains("no puede ser solo números"));
        assertThrows(IllegalArgumentException.class,
                () -> new IniciarSesion(directorio, new CifradorFalso(), " ", "cifrado:cartera2026"));
    }
}
