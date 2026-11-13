package io.github.juand13go.cartera.dominio.estructuras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PilaTest {

    @Test
    void unaPilaNuevaEstaVacia() {
        Pila<Integer> pila = new Pila<>();

        assertTrue(pila.estaVacia());
        assertEquals(0, pila.tamano());
    }

    @Test
    void desapilaEnOrdenInversoAlDeEntrada() {
        Pila<Integer> pila = new Pila<>();
        pila.apilar(1);
        pila.apilar(2);
        pila.apilar(3);

        assertEquals(3, pila.desapilar());
        assertEquals(2, pila.desapilar());
        assertEquals(1, pila.desapilar());
        assertTrue(pila.estaVacia());
    }

    @Test
    void verTopeNoQuitaElElemento() {
        Pila<String> pila = new Pila<>();
        pila.apilar("a");
        pila.apilar("b");

        assertEquals("b", pila.verTope());
        assertEquals("b", pila.verTope());
        assertEquals(2, pila.tamano());
    }

    @Test
    void elTamanoSubeAlApilarYBajaAlDesapilar() {
        Pila<Integer> pila = new Pila<>();
        pila.apilar(10);
        pila.apilar(20);
        assertEquals(2, pila.tamano());

        pila.desapilar();
        assertEquals(1, pila.tamano());
        assertFalse(pila.estaVacia());
    }

    @Test
    void desapilarUnaPilaVaciaLanzaExcepcion() {
        Pila<Integer> pila = new Pila<>();

        assertThrows(IllegalStateException.class, pila::desapilar);
    }

    @Test
    void verTopeDeUnaPilaVaciaLanzaExcepcion() {
        Pila<Integer> pila = new Pila<>();

        assertThrows(IllegalStateException.class, pila::verTope);
    }

    @Test
    void elForEachRecorreDelTopeAlFondoSinModificarLaPila() {
        Pila<Integer> pila = new Pila<>();
        pila.apilar(1);
        pila.apilar(2);
        pila.apilar(3);

        StringBuilder recorrido = new StringBuilder();
        for (Integer numero : pila) {
            recorrido.append(numero);
        }

        assertEquals("321", recorrido.toString());
        assertEquals(3, pila.tamano());
    }

    @Test
    void apilarNullLanzaExcepcion() {
        Pila<String> pila = new Pila<>();

        assertThrows(IllegalArgumentException.class, () -> pila.apilar(null));
    }
}
