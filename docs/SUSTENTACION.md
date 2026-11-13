# Guía de sustentación — cartera-prestamos

Este documento resume cada hito del proyecto para preparar la sustentación oral.

---

## Hito 1: Estructuras de datos

### Qué se construyó

Cuatro estructuras de datos genéricas, escritas a mano, sin usar las colecciones de `java.util`
(`ArrayList`, `HashMap`, `PriorityQueue`, `Stack`...). Todas están en el paquete
`io.github.juand13go.cartera.dominio.estructuras` y cada una tiene su clase de pruebas JUnit 5.

| Clase | Para qué se usa en el sistema | Implementación |
|---|---|---|
| `Pila<T>` | Historial de pagos, para deshacer el último | Nodos con un enlace (`siguiente`) |
| `ListaDoble<T>` | Cronograma de cuotas de cada préstamo | Nodos con dos enlaces (`anterior`, `siguiente`), con `cabeza` y `cola` |
| `TablaHash<K,V>` | Clientes por cédula (búsqueda y login) | Arreglo de cubetas con encadenamiento; se duplica al pasar factor de carga 0.75 |
| `ColaPrioridad<T>` | Agenda de cobros del día y lista de mora | Montículo binario mínimo sobre un arreglo, con un `Comparator` |

Pruebas: `PilaTest` (8), `ListaDobleTest` (14), `TablaHashTest` (12), `ColaPrioridadTest` (10).

### Decisiones comunes a las cuatro

- **Genéricas** (`<T>`, `<K,V>`): la misma clase sirve para cuotas, pagos, clientes o fechas sin repetir código.
- **Implementan `Iterable`**: así se pueden recorrer con `for (Cuota c : cronograma)` en Java y con `th:each` en Thymeleaf.
- **Lo único que se usa de `java.util`** es `Iterator`, `Comparator` y `NoSuchElementException`.
  No son colecciones: son las interfaces que Java exige para el for-each y para comparar, y la excepción que exige el contrato de `Iterator`.
- **Rechazan `null`** con `IllegalArgumentException`, para que `null` nunca tenga dos significados
  (en la tabla, `buscar` devuelve `null` cuando la llave no existe).
- **Llevan un contador `tamano`**: `tamano()` es O(1) porque no hay que recorrer para contar.
- **Sacar o mirar en una estructura vacía** lanza `IllegalStateException`.
- **Los nodos son clases internas privadas** (`private static class Nodo`): nadie de afuera los necesita.

### Pila — por qué y cómo

**Por qué una pila:** deshacer siempre afecta el **último** pago registrado. Eso es exactamente
LIFO (último en entrar, primero en salir).

**Cómo funciona:** `tope` apunta al último nodo apilado; cada nodo apunta al que está debajo.

```java
tope = new Nodo<>(elemento, tope);   // apilar: el nuevo apunta al tope viejo y pasa a ser el tope
tope = tope.siguiente;               // desapilar: el tope pasa a ser el de abajo
```

| Operación | Complejidad | Por qué |
|---|---|---|
| `apilar` | O(1) | se crea un nodo y se mueve `tope`; no se recorre nada |
| `desapilar` | O(1) | se mueve `tope` al siguiente |
| `verTope` | O(1) | se lee `tope` |
| `estaVacia`, `tamano` | O(1) | contador |
| recorrido (for-each) | O(n) | del tope al fondo: del pago más reciente al más viejo |

### ListaDoble — por qué y cómo

**Por qué una lista doble:** el cronograma del rédito **crece una cuota a la vez por el final**,
y a veces hay que leer la última cuota (para calcular la fecha de la siguiente) o quitarla.
Con `cola` y el enlace `anterior`, todo eso es O(1). Además, el cronograma se recorre en orden
(de la cuota más vieja a la más nueva), que es lo natural en una lista.

**Cómo funciona:** cada nodo tiene `anterior` y `siguiente`. La lista guarda `cabeza` (primero) y `cola` (último).

**Caso borde importante:** al eliminar el único elemento, `cabeza` y `cola` deben quedar **ambos** en `null`.
Si solo se actualiza uno, el otro queda apuntando a un nodo que ya no existe en la lista.

| Operación | Complejidad | Por qué |
|---|---|---|
| `agregarAlFinal` | O(1) | se conecta después de `cola` |
| `agregarAlInicio` | O(1) | se conecta antes de `cabeza` |
| `primero`, `ultimo` | O(1) | se leen `cabeza` y `cola` |
| `eliminarPrimero` | O(1) | `cabeza` pasa a su siguiente |
| `eliminarUltimo` | O(1) | `cola` pasa a su **anterior** (en una lista simple sería O(n)) |
| `obtener(indice)` | O(n) | hay que caminar nodo por nodo desde la cabeza |
| `estaVacia`, `tamano` | O(1) | contador |
| recorrido y `alReves()` | O(n) | adelante usa `siguiente`; al revés usa `anterior` |

### TablaHash — por qué y cómo

**Por qué una tabla hash:** el cliente se busca **por cédula** (en el buscador y al hacer login).
Una tabla hash encuentra una llave en O(1) en promedio, sin recorrer todos los clientes.
Con una lista sería O(n).

**Cómo funciona:**
1. Hay un arreglo de 16 **cubetas**. Cada cubeta es una lista enlazada de nodos `(llave, valor, siguiente)`.
2. La cubeta de una llave se calcula con `Math.floorMod(llave.hashCode(), capacidad)`.
   Se usa `floorMod` y no `%` porque `hashCode()` puede ser negativo, y `%` daría un índice negativo.
3. **Colisión:** si dos llaves caen en la misma cubeta, conviven en la lista de esa cubeta (**encadenamiento**).
4. **Factor de carga** = elementos / cubetas. Si pasa de 0.75, el arreglo se **duplica** y cada nodo
   se reubica, porque con otra capacidad cambia la cubeta de cada llave.
   Ejemplo: con 16 cubetas, el elemento 13 hace 13/16 = 0,81 > 0,75 → pasa a 32 cubetas.
5. `guardar` primero busca: si la llave ya existe, reemplaza el valor (una cédula nunca queda dos veces).

| Operación | Complejidad | Por qué |
|---|---|---|
| `guardar` | O(1) promedio, O(n) peor caso | cubeta directa y una lista corta |
| `buscar`, `contiene` | O(1) promedio, O(n) peor caso | igual |
| `eliminar` | O(1) promedio | se desconecta el nodo de la lista de su cubeta |
| `redimensionar` (privado) | O(n), **amortizado O(1)** por inserción | mueve todos los nodos, pero pasa pocas veces |
| `estaVacia`, `tamano`, `capacidad` | O(1) | contador / largo del arreglo |
| recorrido | O(capacidad + n) | visita todas las cubetas, también las vacías |

El **peor caso O(n)** ocurre si todas las llaves cayeran en la misma cubeta. El factor de carga 0.75
mantiene las listas cortas para que eso no pase en la práctica.

### ColaPrioridad — por qué y cómo

**Por qué una cola de prioridad:** el panel necesita siempre **el más urgente primero**: el cobro
más próximo (agenda) o el cliente con más días de atraso (mora). Un montículo da el más urgente en O(1)
y permite agregar o sacar en O(log n). Ordenar toda la lista cada vez costaría O(n log n).

**Cómo funciona:**
- Es un **montículo binario mínimo**: un árbol donde **cada padre es menor o igual que sus hijos**,
  así que el mínimo siempre está en la raíz (posición 0).
- El árbol se guarda en un **arreglo**, sin nodos. Para la posición `i`:
  padre `(i-1)/2`, hijo izquierdo `2i+1`, hijo derecho `2i+2`.
- **Insertar:** se pone al final y **sube** cambiándose con su padre mientras sea menor.
- **Extraer:** se saca la raíz, el último elemento pasa a la raíz y **baja** cambiándose con el
  **menor** de sus hijos hasta quedar en su lugar.
- El orden lo decide un `Comparator` que se pasa al crearla. La misma clase sirve para:
  - agenda: comparar por fecha → sale primero la más próxima;
  - mora: comparador invertido (`.reversed()`) → sale primero el de más días de atraso.
- El constructor recibe `Comparator<? super T>` ("un comparador de T o de un tipo padre de T").
  Hace falta para `LocalDate`, cuyo comparador natural compara `ChronoLocalDate` (su tipo padre).

| Operación | Complejidad | Por qué |
|---|---|---|
| `insertar` | O(log n) | sube como máximo la altura del árbol, que es log n |
| `extraer` | O(log n) | baja como máximo la altura del árbol |
| `verPrimero` | O(1) | siempre está en la posición 0 |
| `estaVacia`, `tamano` | O(1) | contador |
| crecer el arreglo (privado) | O(n), amortizado O(1) | se duplica al llenarse, pasa pocas veces |
| recorrido | O(n log n) | copia el arreglo y va extrayendo de la copia |

**Por qué el recorrido usa una copia:** el arreglo de un montículo **no está ordenado**
(solo cumple padre ≤ hijos). Para mostrar la agenda en orden hay que ir extrayendo, pero eso
vaciaría la cola real. La copia del arreglo ya es un montículo válido, así que se extrae de ella.

### Un detalle de Java que aparece en dos estructuras

Java no permite `new T[10]` ni `new Nodo<K,V>[16]` (los genéricos se borran al compilar y Java
no sabe qué tipo de arreglo crear). Se hace `(T[]) new Object[10]` con `@SuppressWarnings("unchecked")`.
Es seguro porque el arreglo es privado y nunca sale de la clase: solo se meten y sacan elementos de tipo `T`.

### Preguntas que podría hacer el profesor

**1. ¿Por qué usaron una lista doblemente enlazada para el cronograma y no un arreglo o una lista simple?**

Porque el cronograma del rédito no se conoce desde el principio: crece una cuota a la vez por el final,
en cada fecha de corte. En la lista doble, agregar al final es O(1) porque tenemos la referencia a la `cola`.
En un arreglo habría que redimensionar cuando se llena. Frente a una lista simple, la doble tiene el
enlace `anterior`, que permite eliminar la última cuota en O(1) y recorrer el cronograma de la última
a la primera. Con una lista simple, eliminar el último cuesta O(n), porque hay que buscar el penúltimo
desde la cabeza.

**2. ¿Qué pasa en la tabla hash cuando dos cédulas caen en la misma cubeta? ¿Y por qué se redimensiona en 0.75?**

Es una colisión y se resuelve con encadenamiento: cada cubeta es una lista enlazada, y las dos cédulas
quedan en la misma lista. Al buscar se calcula la cubeta y se recorre esa lista comparando con `equals`.
Mientras las listas sean cortas, la búsqueda sigue siendo O(1) en promedio. El factor de carga mide
cuántos elementos hay por cubeta; si pasa de 0.75, las listas empiezan a alargarse, así que se duplica
el arreglo y se reubican todos los nodos. Eso cuesta O(n), pero como el tamaño se duplica, pasa muy pocas
veces: repartido entre todas las inserciones, el costo promedio sigue siendo O(1) (costo amortizado).
En las pruebas se ve con las cadenas `"Aa"` y `"BB"`, que en Java tienen el mismo `hashCode`.

**3. ¿Por qué un montículo para la agenda y la mora, y no ordenar una lista?**

Porque lo que se necesita es sacar siempre el más urgente. En el montículo, ver el más urgente es O(1)
(está en la raíz), e insertar o extraer es O(log n), porque solo se recorre un camino de la raíz a una hoja.
Con 1.000 cobros eso son unos 10 pasos. Ordenar la lista completa cada vez que llega un cobro costaría
O(n log n). Además, como el orden lo da un `Comparator`, la misma clase sirve para la agenda (por fecha)
y para la mora (por días de atraso, invertido), sin escribir dos estructuras.
