package io.github.juand13go.cartera.adaptadores.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioClientes;
import io.github.juand13go.cartera.dominio.modelo.Cliente;

@SpringBootTest
@Transactional
class RepositorioClientesJdbcTest {

    @Autowired
    private RepositorioClientes repositorio;

    private static Cliente cliente(String cedula, String nombre) {
        return new Cliente(cedula, nombre, "3001234567", "Calle 1 # 2-3", "Vecina María",
                new BigDecimal("5000000"), "Paga los viernes", "$2a$10$hashDePrueba");
    }

    @Test
    void guardaYEncuentraUnClienteConTodosSusDatos() {
        repositorio.guardar(cliente("88000001", "Ana Pérez"));

        Cliente leido = repositorio.buscarPorCedula("88000001");

        assertNotNull(leido);
        assertEquals("Ana Pérez", leido.getNombre());
        assertEquals("3001234567", leido.getTelefono());
        assertEquals("Calle 1 # 2-3", leido.getDireccion());
        assertEquals("Vecina María", leido.getReferencia());
        assertEquals(0, new BigDecimal("5000000").compareTo(leido.getCupoMaximo()));
        assertEquals("Paga los viernes", leido.getNotas());
        assertEquals("$2a$10$hashDePrueba", leido.getPinCifrado());
    }

    @Test
    void buscarUnaCedulaQueNoExisteDevuelveNull() {
        assertNull(repositorio.buscarPorCedula("88999999"));
    }

    @Test
    void listarTodosIncluyeLosGuardadosOrdenadosPorNombre() {
        repositorio.guardar(cliente("88000002", "Zoila Zapata"));
        repositorio.guardar(cliente("88000003", "Aarón Álvarez"));

        int posicionAaron = -1;
        int posicionZoila = -1;
        int i = 0;
        for (Cliente c : repositorio.listarTodos()) {
            if (c.getCedula().equals("88000003")) {
                posicionAaron = i;
            }
            if (c.getCedula().equals("88000002")) {
                posicionZoila = i;
            }
            i++;
        }
        assertTrue(posicionAaron >= 0 && posicionZoila >= 0);
        assertTrue(posicionAaron < posicionZoila);
    }

    @Test
    void guardarUnaCedulaRepetidaLanzaExcepcionDelLenguajeYNoDeSpring() {
        repositorio.guardar(cliente("88000004", "Ana Pérez"));

        assertThrows(IllegalArgumentException.class, () -> repositorio.guardar(cliente("88000004", "Otra Persona")));
    }
}
