package io.github.juand13go.cartera.dominio.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lista genérica doblemente enlazada: cada nodo conoce al anterior y al siguiente,
 * y la lista guarda referencias al primero (cabeza) y al último (cola).
 * Se usa para el cronograma de cuotas, que en el rédito crece una cuota a la vez por el final.
 */
public class ListaDoble<T> implements Iterable<T> {

    /** Cada nodo guarda un elemento y los enlaces a sus dos vecinos. */
    private static class Nodo<T> {
        private final T dato;
        private Nodo<T> anterior;
        private Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamano;

    /** Crea una lista vacía. */
    public ListaDoble() {
        this.cabeza = null;
        this.cola = null;
        this.tamano = 0;
    }

    /**
     * Agrega un elemento al final.
     * O(1): se conecta directamente después de la cola, sin recorrer la lista.
     */
    public void agregarAlFinal(T elemento) {
        validarNoNulo(elemento);
        Nodo<T> nuevo = new Nodo<>(elemento);
        if (estaVacia()) {
            cabeza = nuevo;
        } else {
            nuevo.anterior = cola;
            cola.siguiente = nuevo;
        }
        cola = nuevo;
        tamano++;
    }

    /**
     * Agrega un elemento al inicio.
     * O(1): se conecta directamente antes de la cabeza.
     */
    public void agregarAlInicio(T elemento) {
        validarNoNulo(elemento);
        Nodo<T> nuevo = new Nodo<>(elemento);
        if (estaVacia()) {
            cola = nuevo;
        } else {
            nuevo.siguiente = cabeza;
            cabeza.anterior = nuevo;
        }
        cabeza = nuevo;
        tamano++;
    }

    /**
     * Devuelve el primer elemento sin quitarlo.
     * O(1): se lee la cabeza.
     */
    public T primero() {
        validarNoVacia();
        return cabeza.dato;
    }

    /**
     * Devuelve el último elemento sin quitarlo.
     * O(1): se lee la cola.
     */
    public T ultimo() {
        validarNoVacia();
        return cola.dato;
    }

    /**
     * Devuelve el elemento en la posición indicada (la primera es 0).
     * O(n): hay que caminar nodo por nodo desde la cabeza hasta llegar al índice.
     */
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException("Índice " + indice + " fuera de rango, tamaño " + tamano);
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.siguiente;
        }
        return actual.dato;
    }

    /**
     * Quita y devuelve el primer elemento.
     * O(1): la cabeza pasa a ser su siguiente.
     */
    public T eliminarPrimero() {
        validarNoVacia();
        T dato = cabeza.dato;
        cabeza = cabeza.siguiente;
        if (cabeza == null) {
            cola = null;
        } else {
            cabeza.anterior = null;
        }
        tamano--;
        return dato;
    }

    /**
     * Quita y devuelve el último elemento.
     * O(1): gracias al enlace "anterior", la cola pasa a ser el nodo de antes sin recorrer la lista.
     */
    public T eliminarUltimo() {
        validarNoVacia();
        T dato = cola.dato;
        cola = cola.anterior;
        if (cola == null) {
            cabeza = null;
        } else {
            cola.siguiente = null;
        }
        tamano--;
        return dato;
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
     * Recorre la lista del primero al último.
     * O(1) por cada paso, O(n) para recorrerla completa.
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = cabeza;

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

    /**
     * Permite recorrer la lista del último al primero: {@code for (T x : lista.alReves())}.
     * O(1) por cada paso, O(n) completa: usa los enlaces "anterior", que una lista simple no tiene.
     */
    public Iterable<T> alReves() {
        return () -> new Iterator<>() {
            private Nodo<T> actual = cola;

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
                actual = actual.anterior;
                return dato;
            }
        };
    }

    private void validarNoNulo(T elemento) {
        if (elemento == null) {
            throw new IllegalArgumentException("No se puede agregar null a la lista");
        }
    }

    private void validarNoVacia() {
        if (estaVacia()) {
            throw new IllegalStateException("La lista está vacía");
        }
    }
}
