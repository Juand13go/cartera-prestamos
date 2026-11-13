package io.github.juand13go.cartera.dominio.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Pila genérica (LIFO: el último en entrar es el primero en salir),
 * implementada con nodos enlazados.
 * Se usa para el historial de pagos: el pago de arriba es el último y es el que se deshace.
 */
public class Pila<T> implements Iterable<T> {

    /** Cada nodo guarda un elemento y apunta al nodo que está debajo de él. */
    private static class Nodo<T> {
        private final T dato;
        private final Nodo<T> siguiente;

        Nodo(T dato, Nodo<T> siguiente) {
            this.dato = dato;
            this.siguiente = siguiente;
        }
    }

    private Nodo<T> tope;
    private int tamano;

    /** Crea una pila vacía. */
    public Pila() {
        this.tope = null;
        this.tamano = 0;
    }

    /**
     * Pone un elemento encima de la pila.
     * O(1): el nodo nuevo apunta al tope viejo y pasa a ser el tope; no se recorre nada.
     */
    public void apilar(T elemento) {
        if (elemento == null) {
            throw new IllegalArgumentException("No se puede apilar null");
        }
        tope = new Nodo<>(elemento, tope);
        tamano++;
    }

    /**
     * Quita y devuelve el elemento de encima.
     * O(1): el tope pasa a ser el nodo que estaba debajo.
     */
    public T desapilar() {
        if (estaVacia()) {
            throw new IllegalStateException("La pila está vacía");
        }
        T dato = tope.dato;
        tope = tope.siguiente;
        tamano--;
        return dato;
    }

    /**
     * Devuelve el elemento de encima sin quitarlo.
     * O(1): solo se lee el tope.
     */
    public T verTope() {
        if (estaVacia()) {
            throw new IllegalStateException("La pila está vacía");
        }
        return tope.dato;
    }

    /** O(1): basta con mirar el contador. */
    public boolean estaVacia() {
        return tamano == 0;
    }

    /** O(1): se lleva un contador, no hay que recorrer para contar. */
    public int tamano() {
        return tamano;
    }

    /**
     * Recorre la pila del tope al fondo (del pago más reciente al más viejo) sin modificarla.
     * O(1) por cada paso, O(n) para recorrerla completa.
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = tope;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public T next() {
                if (actual == null) {
                    throw new NoSuchElementException();
                }
                T dato = actual.dato;
                actual = actual.siguiente;
                return dato;
            }
        };
    }
}
