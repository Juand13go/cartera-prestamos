package io.github.juand13go.cartera.dominio.estructuras;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Cola de prioridad genérica implementada como montículo binario mínimo sobre un arreglo.
 * Siempre sale primero el elemento "menor" según el comparador que se pasa al crearla:
 * - agenda de cobros: se compara por fecha, sale primero la más cercana;
 * - lista de mora: se pasa el comparador invertido, sale primero el de más días de atraso.
 *
 * El árbol vive dentro del arreglo, sin nodos: para la posición i,
 * el padre está en (i - 1) / 2, el hijo izquierdo en 2i + 1 y el derecho en 2i + 2.
 * Regla del montículo: cada padre es menor o igual que sus hijos, así que el mínimo está en la posición 0.
 */
public class ColaPrioridad<T> implements Iterable<T> {

    private static final int CAPACIDAD_INICIAL = 10;

    private final Comparator<? super T> comparador;
    private T[] elementos;
    private int tamano;

    /** Crea una cola vacía que ordena con el comparador dado. */
    public ColaPrioridad(Comparator<? super T> comparador) {
        if (comparador == null) {
            throw new IllegalArgumentException("El comparador no puede ser null");
        }
        this.comparador = comparador;
        this.elementos = crearArreglo(CAPACIDAD_INICIAL);
        this.tamano = 0;
    }

    /**
     * Agrega un elemento en su lugar según la prioridad.
     * O(log n): se pone al final y "sube" cambiándose con su padre mientras sea menor;
     * como el árbol está balanceado, tiene altura log n y sube como máximo esa cantidad de veces.
     * Si el arreglo está lleno se duplica (O(n), pero pocas veces: amortizado sigue siendo O(log n)).
     */
    public void insertar(T elemento) {
        if (elemento == null) {
            throw new IllegalArgumentException("No se puede insertar null");
        }
        if (tamano == elementos.length) {
            crecer();
        }
        elementos[tamano] = elemento;
        subir(tamano);
        tamano++;
    }

    /**
     * Quita y devuelve el elemento de mayor prioridad (el menor según el comparador).
     * O(log n): se saca la raíz, el último elemento pasa a la raíz y "baja"
     * cambiándose con el menor de sus hijos hasta quedar en su lugar.
     */
    public T extraer() {
        T primero = verPrimero();
        tamano--;
        elementos[0] = elementos[tamano];
        elementos[tamano] = null;
        if (tamano > 0) {
            bajar(0);
        }
        return primero;
    }

    /**
     * Devuelve el elemento de mayor prioridad sin quitarlo.
     * O(1): por la regla del montículo, siempre está en la posición 0.
     */
    public T verPrimero() {
        if (estaVacia()) {
            throw new IllegalStateException("La cola de prioridad está vacía");
        }
        return elementos[0];
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
     * Recorre los elementos en orden de prioridad sin modificar esta cola.
     * Trabaja sobre una copia y va extrayendo de ella: O(n) para copiar y O(log n) por cada paso,
     * O(n log n) para recorrerla completa.
     * (Recorrer el arreglo tal cual no sirve: el montículo no guarda los elementos ordenados.)
     */
    @Override
    public Iterator<T> iterator() {
        ColaPrioridad<T> copia = new ColaPrioridad<>(comparador);
        copia.elementos = crearArreglo(elementos.length);
        for (int i = 0; i < tamano; i++) {
            copia.elementos[i] = elementos[i];
        }
        copia.tamano = tamano;

        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return !copia.estaVacia();
            }

            @Override
            public T next() {
                if (copia.estaVacia()) {
                    throw new NoSuchElementException();
                }
                return copia.extraer();
            }
        };
    }

    /** Sube el elemento de la posición i mientras sea menor que su padre. */
    private void subir(int i) {
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (esMenor(elementos[i], elementos[padre])) {
                intercambiar(i, padre);
                i = padre;
            } else {
                return;
            }
        }
    }

    /** Baja el elemento de la posición i mientras alguno de sus hijos sea menor que él. */
    private void bajar(int i) {
        while (true) {
            int izquierdo = 2 * i + 1;
            int derecho = 2 * i + 2;
            int menor = i;
            if (izquierdo < tamano && esMenor(elementos[izquierdo], elementos[menor])) {
                menor = izquierdo;
            }
            if (derecho < tamano && esMenor(elementos[derecho], elementos[menor])) {
                menor = derecho;
            }
            if (menor == i) {
                return;
            }
            intercambiar(i, menor);
            i = menor;
        }
    }

    private boolean esMenor(T a, T b) {
        return comparador.compare(a, b) < 0;
    }

    private void intercambiar(int i, int j) {
        T temporal = elementos[i];
        elementos[i] = elementos[j];
        elementos[j] = temporal;
    }

    /** Duplica el tamaño del arreglo copiando los elementos. O(n). */
    private void crecer() {
        T[] nuevo = crearArreglo(elementos.length * 2);
        for (int i = 0; i < tamano; i++) {
            nuevo[i] = elementos[i];
        }
        elementos = nuevo;
    }

    /** Java no permite "new T[n]"; se crea un Object[] y se convierte. Es seguro porque el arreglo no sale de la clase. */
    @SuppressWarnings("unchecked")
    private T[] crearArreglo(int capacidad) {
        return (T[]) new Object[capacidad];
    }
}
