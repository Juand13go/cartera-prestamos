package io.github.juand13go.cartera.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ClienteTest {

    private static final BigDecimal CUPO = new BigDecimal("5000000");
    private static final String PIN_CIFRADO = "$2a$10$hashDePrueba";

    private static Cliente cliente(String cedula, String nombre, String telefono, BigDecimal cupo, String pin) {
        return new Cliente(cedula, nombre, telefono, "Calle 1 # 2-3", "Vecina María", cupo, "Paga los viernes", pin);
    }

    @Test
    void guardaTodosSusDatos() {
        Cliente cliente = cliente("1020304050", "Ana Pérez", "3001234567", CUPO, PIN_CIFRADO);

        assertEquals("1020304050", cliente.getCedula());
        assertEquals("Ana Pérez", cliente.getNombre());
        assertEquals("3001234567", cliente.getTelefono());
        assertEquals("Calle 1 # 2-3", cliente.getDireccion());
        assertEquals("Vecina María", cliente.getReferencia());
        assertEquals(CUPO, cliente.getCupoMaximo());
        assertEquals("Paga los viernes", cliente.getNotas());
        assertEquals(PIN_CIFRADO, cliente.getPinCifrado());
    }

    @Test
    void losDatosOpcionalesVaciosQuedanComoTextoVacio() {
        Cliente cliente = new Cliente("1020304050", "Ana Pérez", "3001234567", null, null, CUPO, null, PIN_CIFRADO);

        assertEquals("", cliente.getDireccion());
        assertEquals("", cliente.getReferencia());
        assertEquals("", cliente.getNotas());
    }

    @Test
    void elNombreSeGuardaSinEspaciosSobrantes() {
        Cliente cliente = cliente("1020304050", "  Ana Pérez  ", "3001234567", CUPO, PIN_CIFRADO);

        assertEquals("Ana Pérez", cliente.getNombre());
    }

    @Test
    void laCedulaDebeTenerEntre6Y10Digitos() {
        assertThrows(IllegalArgumentException.class, () -> cliente("12345", "Ana", "3001234567", CUPO, PIN_CIFRADO));
        assertThrows(IllegalArgumentException.class, () -> cliente("12345678901", "Ana", "3001234567", CUPO, PIN_CIFRADO));
        assertThrows(IllegalArgumentException.class, () -> cliente("10.203.040", "Ana", "3001234567", CUPO, PIN_CIFRADO));
        assertThrows(IllegalArgumentException.class, () -> cliente(null, "Ana", "3001234567", CUPO, PIN_CIFRADO));
    }

    @Test
    void elNombreEsObligatorio() {
        assertThrows(IllegalArgumentException.class, () -> cliente("1020304050", " ", "3001234567", CUPO, PIN_CIFRADO));
    }

    @Test
    void elTelefonoDebeTener10Digitos() {
        assertThrows(IllegalArgumentException.class, () -> cliente("1020304050", "Ana", "300123456", CUPO, PIN_CIFRADO));
        assertThrows(IllegalArgumentException.class, () -> cliente("1020304050", "Ana", "300 123 4567", CUPO, PIN_CIFRADO));
    }

    @Test
    void elCupoNoPuedeSerNegativoPeroSiCero() {
        assertThrows(IllegalArgumentException.class,
                () -> cliente("1020304050", "Ana", "3001234567", new BigDecimal("-1"), PIN_CIFRADO));
        assertEquals(BigDecimal.ZERO, cliente("1020304050", "Ana", "3001234567", BigDecimal.ZERO, PIN_CIFRADO).getCupoMaximo());
    }

    @Test
    void elPinEsObligatorio() {
        assertThrows(IllegalArgumentException.class, () -> cliente("1020304050", "Ana", "3001234567", CUPO, ""));
    }
}
