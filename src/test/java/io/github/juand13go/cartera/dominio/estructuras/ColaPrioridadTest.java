package io.github.juand13go.cartera.dominio.estructuras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Comparator;

import org.junit.jupiter.api.Test;

class ColaPrioridadTest {

    private static ColaPrioridad<Integer> colaDeMenorAMayor() {
        return new ColaPrioridad<>(Comparator.naturalOrder());
    }

    /** Arma un texto extrayendo todos los elementos de la cola. */
    private static String extraerTodo(ColaPrioridad<Integer> cola) {
        StringBuilder texto = new StringBuilder();
        while (!cola.estaVacia()) {
            texto.append(cola.extraer());
        }
        return texto.toString();
    }

    @Test
    void unaColaNuevaEstaVacia() {
        ColaPrioridad<Integer> cola = colaDeMenorAMayor();

        assertTrue(cola.estaVacia());
        assertEquals(0, cola.tamano());
    }

    @Test
    void extraeDeMenorAMayor() {
        ColaPrioridad<Integer> cola = colaDeMenorAMayor();
        cola.insertar(5);
        cola.insertar(1);
        cola.insertar(4);
        cola.insertar(2);
        cola.insertar(3);

        assertEquals("12345", extraerTodo(cola));
    }

    @Test
    void conElComparadorInvertidoSalePrimeroElMayor() {
        // Así se arma la lista de mora: primero el de más días de atraso.
        ColaPrioridad<Integer> diasDeAtraso = new ColaPrioridad<>(Comparator.reverseOrder());
        diasDeAtraso.insertar(3);
        diasDeAtraso.insertar(40);
        diasDeAtraso.insertar(12);

        assertEquals(40, diasDeAtraso.extraer());
        assertEquals(12, diasDeAtraso.extraer());
        assertEquals(3, diasDeAtraso.extraer());
    }

    @Test
    void losValoresRepetidosSalenTodos() {
        ColaPrioridad<Integer> cola = colaDeMenorAMayor();
        cola.insertar(2);
        cola.insertar(1);
        cola.insertar(2);
        cola.insertar(1);

        assertEquals("1122", extraerTodo(cola));
    }

    @Test
    void verPrimeroNoQuitaElElemento() {
        ColaPrioridad<Integer> cola = colaDeMenorAMayor();
        cola.insertar(7);
        cola.insertar(3);

        assertEquals(3, cola.verPrimero());
        assertEquals(3, cola.verPrimero());
        assertEquals(2, cola.tamano());
    }

    @Test
    void extraerOVerPrimeroEnColaVaciaLanzaExcepcion() {
        ColaPrioridad<Integer> cola = colaDeMenorAMayor();

        assertThrows(IllegalStateException.class, cola::extraer);
        assertThrows(IllegalStateException.class, cola::verPrimero);
    }

    @Test
    void creceMasAllaDeLaCapacidadInicialYSigueOrdenando() {
        ColaPrioridad<Integer> cola = colaDeMenorAMayor();
        // (i * 37) % 100 recorre los números del 0 al 99 una sola vez, pero desordenados.
        for (int i = 0; i < 100; i++) {
            cola.insertar((i * 37) % 100);
        }

        assertEquals(100, cola.tamano());
        for (int esperado = 0; esperado < 100; esperado++) {
            assertEquals(esperado, cola.extraer());
        }
        assertTrue(cola.estaVacia());
    }

    @Test
    void elForEachRecorreEnOrdenDePrioridadSinVaciarLaCola() {
        ColaPrioridad<Integer> cola = colaDeMenorAMayor();
        cola.insertar(30);
        cola.insertar(10);
        cola.insertar(20);

        StringBuilder recorrido = new StringBuilder();
        for (Integer numero : cola) {
            recorrido.append(numero).append(' ');
        }

        assertEquals("10 20 30 ", recorrido.toString());
        assertEquals(3, cola.tamano());
        assertEquals(10, cola.verPrimero());
    }

    @Test
    void ordenaFechasDeCobroDeLaMasProximaALaMasLejana() {
        ColaPrioridad<LocalDate> agenda = new ColaPrioridad<>(Comparator.naturalOrder());
        agenda.insertar(LocalDate.of(2026, 12, 10));
        agenda.insertar(LocalDate.of(2026, 11, 15));
        agenda.insertar(LocalDate.of(2027, 1, 5));
        agenda.insertar(LocalDate.of(2026, 11, 30));

        assertEquals(LocalDate.of(2026, 11, 15), agenda.extraer());
        assertEquals(LocalDate.of(2026, 11, 30), agenda.extraer());
        assertEquals(LocalDate.of(2026, 12, 10), agenda.extraer());
        assertEquals(LocalDate.of(2027, 1, 5), agenda.extraer());
    }

    @Test
    void insertarNullLanzaExcepcion() {
        ColaPrioridad<Integer> cola = colaDeMenorAMayor();

        assertThrows(IllegalArgumentException.class, () -> cola.insertar(null));
    }
}
