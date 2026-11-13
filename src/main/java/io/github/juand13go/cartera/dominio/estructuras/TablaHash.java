package io.github.juand13go.cartera.dominio.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Tabla hash genérica que asocia llaves con valores (por ejemplo, cédula → cliente).
 * Es un arreglo de "cubetas"; cada cubeta es una lista enlazada simple de nodos
 * (encadenamiento), para que dos llaves que caen en la misma cubeta puedan convivir.
 * Cuando el factor de carga (elementos / cubetas) pasa de 0.75, el arreglo se duplica.
 */
public class TablaHash<K, V> implements Iterable<V> {

    private static final int CAPACIDAD_INICIAL = 16;
    private static final double FACTOR_DE_CARGA_MAXIMO = 0.75;

    /** Cada nodo guarda una pareja llave-valor y apunta al siguiente nodo de su misma cubeta. */
    private static class Nodo<K, V> {
        private final K llave;
        private V valor;
        private Nodo<K, V> siguiente;

        Nodo(K llave, V valor, Nodo<K, V> siguiente) {
            this.llave = llave;
            this.valor = valor;
            this.siguiente = siguiente;
        }
    }

    private Nodo<K, V>[] cubetas;
    private int tamano;

    /** Crea una tabla vacía con 16 cubetas. */
    public TablaHash() {
        this.cubetas = crearCubetas(CAPACIDAD_INICIAL);
        this.tamano = 0;
    }

    /**
     * Guarda el valor bajo la llave; si la llave ya existía, reemplaza su valor.
     * O(1) en promedio: se calcula la cubeta y se recorre su lista, que es corta.
     * O(n) en el peor caso, si todas las llaves cayeran en la misma cubeta.
     * Cuando toca redimensionar cuesta O(n), pero pasa tan pocas veces que en promedio sigue siendo O(1) (amortizado).
     */
    public void guardar(K llave, V valor) {
        validarLlave(llave);
        if (valor == null) {
            throw new IllegalArgumentException("El valor no puede ser null");
        }
        Nodo<K, V> existente = buscarNodo(llave);
        if (existente != null) {
            existente.valor = valor;
            return;
        }
        int indice = indiceDe(llave, cubetas.length);
        cubetas[indice] = new Nodo<>(llave, valor, cubetas[indice]);
        tamano++;
        if ((double) tamano / cubetas.length > FACTOR_DE_CARGA_MAXIMO) {
            redimensionar();
        }
    }

    /**
     * Devuelve el valor guardado bajo la llave, o null si la llave no está.
     * O(1) en promedio, O(n) en el peor caso (misma razón que guardar).
     */
    public V buscar(K llave) {
        validarLlave(llave);
        Nodo<K, V> nodo = buscarNodo(llave);
        return nodo == null ? null : nodo.valor;
    }

    /** O(1) en promedio: es un buscar. */
    public boolean contiene(K llave) {
        return buscar(llave) != null;
    }

    /**
     * Quita la llave y devuelve el valor que tenía, o null si la llave no estaba.
     * O(1) en promedio: se busca en la lista de su cubeta y se desconecta el nodo.
     */
    public V eliminar(K llave) {
        validarLlave(llave);
        int indice = indiceDe(llave, cubetas.length);
        Nodo<K, V> anterior = null;
        Nodo<K, V> actual = cubetas[indice];
        while (actual != null) {
            if (actual.llave.equals(llave)) {
                if (anterior == null) {
                    cubetas[indice] = actual.siguiente;
                } else {
                    anterior.siguiente = actual.siguiente;
                }
                tamano--;
                return actual.valor;
            }
            anterior = actual;
            actual = actual.siguiente;
        }
        return null;
    }

    /** O(1): basta con mirar el contador. */
    public boolean estaVacia() {
        return tamano == 0;
    }

    /** O(1): se lleva un contador de parejas guardadas. */
    public int tamano() {
        return tamano;
    }

    /** O(1): número de cubetas actual; sirve para comprobar la redimensión. */
    public int capacidad() {
        return cubetas.length;
    }

    /**
     * Recorre los valores guardados, cubeta por cubeta. El orden no es el de inserción.
     * O(capacidad + n) para recorrerla completa: se visitan todas las cubetas, aun las vacías.
     */
    @Override
    public Iterator<V> iterator() {
        return new Iterator<>() {
            private int cubetaActual = 0;
            private Nodo<K, V> actual = siguienteNodoDesde(null);

            /** Devuelve el nodo que sigue a "nodo"; si se acaba su cubeta, salta a la próxima que tenga nodos. */
            private Nodo<K, V> siguienteNodoDesde(Nodo<K, V> nodo) {
                if (nodo != null && nodo.siguiente != null) {
                    return nodo.siguiente;
                }
                if (nodo != null) {
                    cubetaActual++;
                }
                while (cubetaActual < cubetas.length && cubetas[cubetaActual] == null) {
                    cubetaActual++;
                }
                return cubetaActual < cubetas.length ? cubetas[cubetaActual] : null;
            }

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public V next() {
                if (actual == null) {
                    throw new NoSuchElementException();
                }
                V valor = actual.valor;
                actual = siguienteNodoDesde(actual);
                return valor;
            }
        };
    }

    /**
     * Calcula en qué cubeta va una llave.
     * Se usa floorMod y no % porque hashCode() puede ser negativo y % daría un índice negativo.
     */
    private int indiceDe(K llave, int capacidad) {
        return Math.floorMod(llave.hashCode(), capacidad);
    }

    /** Recorre la lista de la cubeta de la llave buscando un nodo con esa llave. */
    private Nodo<K, V> buscarNodo(K llave) {
        Nodo<K, V> actual = cubetas[indiceDe(llave, cubetas.length)];
        while (actual != null) {
            if (actual.llave.equals(llave)) {
                return actual;
            }
            actual = actual.siguiente;
        }
        return null;
    }

    /**
     * Duplica el número de cubetas y reubica cada nodo.
     * O(n): hay que mover todos los nodos, porque con otra capacidad cambia la cubeta de cada llave.
     */
    private void redimensionar() {
        Nodo<K, V>[] viejas = cubetas;
        cubetas = crearCubetas(viejas.length * 2);
        for (Nodo<K, V> cabeza : viejas) {
            Nodo<K, V> actual = cabeza;
            while (actual != null) {
                Nodo<K, V> siguiente = actual.siguiente;
                int indice = indiceDe(actual.llave, cubetas.length);
                actual.siguiente = cubetas[indice];
                cubetas[indice] = actual;
                actual = siguiente;
            }
        }
    }

    /** Java no permite "new Nodo<K, V>[n]"; se crea sin tipo y se convierte. Es seguro porque el arreglo no sale de la clase. */
    @SuppressWarnings("unchecked")
    private Nodo<K, V>[] crearCubetas(int capacidad) {
        return (Nodo<K, V>[]) new Nodo[capacidad];
    }

    private void validarLlave(K llave) {
        if (llave == null) {
            throw new IllegalArgumentException("La llave no puede ser null");
        }
    }
}
