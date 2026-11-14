package io.github.juand13go.cartera.adaptadores.seguridad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CifradorPinBCryptTest {

    private final CifradorPinBCrypt cifrador = new CifradorPinBCrypt();

    @Test
    void elTextoCifradoNoContieneElPin() {
        String cifrado = cifrador.cifrar("1234");

        assertFalse(cifrado.contains("1234"));
        assertTrue(cifrado.startsWith("$2"));
    }

    @Test
    void reconoceElPinCorrectoYRechazaUnoEquivocado() {
        String cifrado = cifrador.cifrar("1234");

        assertTrue(cifrador.coincide("1234", cifrado));
        assertFalse(cifrador.coincide("4321", cifrado));
    }

    @Test
    void elMismoPinDaUnTextoDistintoCadaVezPorLaSal() {
        String primero = cifrador.cifrar("1234");
        String segundo = cifrador.cifrar("1234");

        assertNotEquals(primero, segundo);
        assertTrue(cifrador.coincide("1234", primero));
        assertTrue(cifrador.coincide("1234", segundo));
    }
}
