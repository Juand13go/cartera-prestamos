package io.github.juand13go.cartera.dominio.estructuras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TablaHashTest {

    @Test
    void unaTablaNuevaEstaVacia() {
        TablaHash<String, String> tabla = new TablaHash<>();

        assertTrue(tabla.estaVacia());
        assertEquals(0, tabla.tamano());
        assertEquals(16, tabla.capacidad());
    }

    @Test
    void guardaYBuscaPorLlave() {
        TablaHash<String, String> tabla = new TablaHash<>();
        tabla.guardar("1020304050", "Ana");
        tabla.guardar("9988776655", "Luis");

        assertEquals("Ana", tabla.buscar("1020304050"));
        assertEquals("Luis", tabla.buscar("9988776655"));
        assertEquals(2, tabla.tamano());
    }

    @Test
    void buscarUnaLlaveQueNoExisteDevuelveNull() {
        TablaHash<String, String> tabla = new TablaHash<>();
        tabla.guardar("111", "Ana");

        assertNull(tabla.buscar("222"));
        assertFalse(tabla.contiene("222"));
        assertTrue(tabla.contiene("111"));
    }

    @Test
    void guardarConLlaveRepetidaReemplazaElValorSinAumentarElTamano() {
        TablaHash<String, String> tabla = new TablaHash<>();
        tabla.guardar("111", "Ana");
        tabla.guardar("111", "Ana María");

        assertEquals("Ana María", tabla.buscar("111"));
        assertEquals(1, tabla.tamano());
    }

    @Test
    void eliminarQuitaLaLlaveYDevuelveSuValor() {
        TablaHash<String, String> tabla = new TablaHash<>();
        tabla.guardar("111", "Ana");
        tabla.guardar("222", "Luis");

        assertEquals("Ana", tabla.eliminar("111"));
        assertNull(tabla.buscar("111"));
        assertEquals(1, tabla.tamano());
    }

    @Test
    void eliminarUnaLlaveQueNoExisteDevuelveNull() {
        TablaHash<String, String> tabla = new TablaHash<>();
        tabla.guardar("111", "Ana");

        assertNull(tabla.eliminar("999"));
        assertEquals(1, tabla.tamano());
    }

    @Test
    void dosLlavesConElMismoHashConvivenEnLaMismaCubeta() {
        // En Java, "Aa" y "BB" tienen exactamente el mismo hashCode (2112): siempre chocan.
        assertEquals("Aa".hashCode(), "BB".hashCode());
        TablaHash<String, Integer> tabla = new TablaHash<>();
        tabla.guardar("Aa", 1);
        tabla.guardar("BB", 2);

        assertEquals(1, tabla.buscar("Aa"));
        assertEquals(2, tabla.buscar("BB"));

        tabla.eliminar("Aa");
        assertNull(tabla.buscar("Aa"));
        assertEquals(2, tabla.buscar("BB"));
    }

    @Test
    void seRedimensionaAlPasarElFactorDeCarga() {
        TablaHash<Integer, Integer> tabla = new TablaHash<>();
        for (int i = 1; i <= 12; i++) {
            tabla.guardar(i, i);
        }
        assertEquals(16, tabla.capacidad()); // 12 / 16 = 0.75, no lo supera

        tabla.guardar(13, 13);
        assertEquals(32, tabla.capacidad()); // 13 / 16 = 0.81 > 0.75
    }

    @Test
    void despuesDeVariasRedimensionesNoSePierdeNingunElemento() {
        TablaHash<String, Integer> tabla = new TablaHash<>();
        for (int i = 0; i < 1000; i++) {
            tabla.guardar("cedula-" + i, i);
        }

        assertEquals(1000, tabla.tamano());
        for (int i = 0; i < 1000; i++) {
            assertEquals(i, tabla.buscar("cedula-" + i));
        }
    }

    @Test
    void elForEachRecorreTodosLosValoresUnaSolaVez() {
        TablaHash<Integer, Integer> tabla = new TablaHash<>();
        int sumaEsperada = 0;
        for (int i = 1; i <= 50; i++) {
            tabla.guardar(i, i);
            sumaEsperada += i;
        }

        int suma = 0;
        int cantidad = 0;
        for (Integer valor : tabla) {
            suma += valor;
            cantidad++;
        }

        assertEquals(50, cantidad);
        assertEquals(sumaEsperada, suma);
    }

    @Test
    void elForEachDeUnaTablaVaciaNoEntregaNada() {
        TablaHash<String, String> tabla = new TablaHash<>();

        assertFalse(tabla.iterator().hasNext());
    }

    @Test
    void llavesOValoresNullLanzanExcepcion() {
        TablaHash<String, String> tabla = new TablaHash<>();

        assertThrows(IllegalArgumentException.class, () -> tabla.guardar(null, "Ana"));
        assertThrows(IllegalArgumentException.class, () -> tabla.guardar("111", null));
        assertThrows(IllegalArgumentException.class, () -> tabla.buscar(null));
        assertThrows(IllegalArgumentException.class, () -> tabla.eliminar(null));
    }
}
