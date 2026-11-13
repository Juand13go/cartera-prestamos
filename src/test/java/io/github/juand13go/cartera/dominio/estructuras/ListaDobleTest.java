package io.github.juand13go.cartera.dominio.estructuras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ListaDobleTest {

    /** Arma un texto con los elementos en el orden en que los entrega el recorrido. */
    private static String recorrer(Iterable<Integer> recorrido) {
        StringBuilder texto = new StringBuilder();
        for (Integer numero : recorrido) {
            texto.append(numero);
        }
        return texto.toString();
    }

    private static ListaDoble<Integer> listaConUnoDosTres() {
        ListaDoble<Integer> lista = new ListaDoble<>();
        lista.agregarAlFinal(1);
        lista.agregarAlFinal(2);
        lista.agregarAlFinal(3);
        return lista;
    }

    @Test
    void unaListaNuevaEstaVacia() {
        ListaDoble<Integer> lista = new ListaDoble<>();

        assertTrue(lista.estaVacia());
        assertEquals(0, lista.tamano());
    }

    @Test
    void agregarAlFinalConservaElOrdenDeLlegada() {
        ListaDoble<Integer> lista = listaConUnoDosTres();

        assertEquals("123", recorrer(lista));
        assertEquals(3, lista.tamano());
    }

    @Test
    void agregarAlInicioPoneElElementoAdelante() {
        ListaDoble<Integer> lista = listaConUnoDosTres();
        lista.agregarAlInicio(0);

        assertEquals("0123", recorrer(lista));
        assertEquals(0, lista.primero());
    }

    @Test
    void primeroYUltimoSonLosExtremos() {
        ListaDoble<Integer> lista = listaConUnoDosTres();

        assertEquals(1, lista.primero());
        assertEquals(3, lista.ultimo());
    }

    @Test
    void conUnSoloElementoPrimeroYUltimoSonElMismo() {
        ListaDoble<Integer> lista = new ListaDoble<>();
        lista.agregarAlInicio(7);

        assertEquals(7, lista.primero());
        assertEquals(7, lista.ultimo());
    }

    @Test
    void obtenerDevuelveElElementoDelIndice() {
        ListaDoble<Integer> lista = listaConUnoDosTres();

        assertEquals(1, lista.obtener(0));
        assertEquals(2, lista.obtener(1));
        assertEquals(3, lista.obtener(2));
    }

    @Test
    void obtenerConIndiceFueraDeRangoLanzaExcepcion() {
        ListaDoble<Integer> lista = listaConUnoDosTres();

        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(3));
    }

    @Test
    void eliminarPrimeroDevuelveElPrimeroYBajaElTamano() {
        ListaDoble<Integer> lista = listaConUnoDosTres();

        assertEquals(1, lista.eliminarPrimero());
        assertEquals(2, lista.tamano());
        assertEquals("23", recorrer(lista));
    }

    @Test
    void eliminarUltimoDevuelveElUltimoYBajaElTamano() {
        ListaDoble<Integer> lista = listaConUnoDosTres();

        assertEquals(3, lista.eliminarUltimo());
        assertEquals(2, lista.tamano());
        assertEquals("12", recorrer(lista));
    }

    @Test
    void alEliminarElUnicoElementoLaListaQuedaVaciaYSePuedeReusar() {
        ListaDoble<Integer> lista = new ListaDoble<>();
        lista.agregarAlFinal(5);
        lista.eliminarUltimo();

        assertTrue(lista.estaVacia());
        assertEquals("", recorrer(lista));
        assertEquals("", recorrer(lista.alReves()));

        lista.agregarAlFinal(8);
        lista.eliminarPrimero();
        assertTrue(lista.estaVacia());

        lista.agregarAlFinal(9);
        assertEquals(9, lista.primero());
        assertEquals(9, lista.ultimo());
    }

    @Test
    void operacionesSobreListaVaciaLanzanExcepcion() {
        ListaDoble<Integer> lista = new ListaDoble<>();

        assertThrows(IllegalStateException.class, lista::eliminarPrimero);
        assertThrows(IllegalStateException.class, lista::eliminarUltimo);
        assertThrows(IllegalStateException.class, lista::primero);
        assertThrows(IllegalStateException.class, lista::ultimo);
    }

    @Test
    void alRevesRecorreDelUltimoAlPrimero() {
        ListaDoble<Integer> lista = listaConUnoDosTres();

        assertEquals("321", recorrer(lista.alReves()));
    }

    @Test
    void losEnlacesQuedanBienDespuesDeAgregarYEliminarPorAmbosLados() {
        ListaDoble<Integer> lista = listaConUnoDosTres();
        lista.agregarAlInicio(0);
        lista.eliminarUltimo();
        lista.agregarAlFinal(4);
        lista.eliminarPrimero();

        assertEquals("124", recorrer(lista));
        assertEquals("421", recorrer(lista.alReves()));
    }

    @Test
    void agregarNullLanzaExcepcion() {
        ListaDoble<Integer> lista = new ListaDoble<>();

        assertThrows(IllegalArgumentException.class, () -> lista.agregarAlFinal(null));
        assertThrows(IllegalArgumentException.class, () -> lista.agregarAlInicio(null));
    }
}
