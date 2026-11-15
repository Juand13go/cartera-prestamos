# Guía de sustentación — cartera-prestamos

Este documento resume cada hito del proyecto para preparar la sustentación oral.
Las secciones de cada hito se escribieron al terminarlo; cuando algo cambió después, hay una nota que lo dice.

## Índice

- [Resumen en una página](#resumen-en-una-página) — para repasar la noche anterior
- [Hito 1: Estructuras de datos](#hito-1-estructuras-de-datos)
- [Hito 2: Dominio del rédito](#hito-2-dominio-del-rédito)
- [Hito 3: Políticas del contrato](#hito-3-políticas-del-contrato)
- [Hito 4: Persistencia y casos de uso](#hito-4-persistencia-y-casos-de-uso)
- [Hito 5: Web del prestamista](#hito-5-web-del-prestamista)
- [Hito 6: Login, roles y portal del cliente](#hito-6-login-roles-y-portal-del-cliente)
- [Preguntas generales del proyecto](#preguntas-generales-del-proyecto)
- [Lista de verificación contra el enunciado](#lista-de-verificación-contra-el-enunciado)

---

## Resumen en una página

**Qué es:** sistema web para un prestamista que maneja préstamos tipo rédito (cada periodo se paga el interés; lo que
se pague de más va a capital; se cierra cuando no se debe nada). Dos roles: prestamista (todo) y cliente (solo lectura,
solo lo suyo). 330 pruebas automáticas.

**Las cuatro estructuras (hechas a mano, genéricas, `Iterable`):**

| Estructura | Dónde se usa | Operación clave | Complejidad |
|---|---|---|---|
| `ListaDoble<T>` | Cronograma de cuotas (crece una a la vez) | `agregarAlFinal`, `ultimo`, `eliminarUltimo` | O(1) |
| `TablaHash<K,V>` | Clientes por cédula (buscador y login); condonaciones | `guardar`, `buscar` | O(1) promedio (factor de carga 0,75) |
| `ColaPrioridad<T>` | Agenda de cobros (por fecha) y lista de mora (por atraso, comparador invertido) | `insertar`, `extraer` | O(log n) |
| `Pila<T>` | Historial de pagos; deshacer el último | `apilar`, `desapilar` | O(1) |

**Cinco ideas del diseño que conviene saber explicar:**
1. **La base de datos guarda, las estructuras calculan.** Solo se guardan clientes, contratos, pagos y condonaciones.
   Al leer un préstamo se reconstruye repitiendo sus pagos en orden; cuotas, estados, multas y saldos se calculan.
2. **Deshacer** = desapilar el último pago y volver a aplicar los demás desde el contrato.
3. **Strategy** en `Frecuencia` y `PoliticaMora`: el préstamo solo conoce la interfaz; agregar opciones no lo cambia.
4. **Hexagonal ligera:** el dominio es Java puro (sin Spring, sin SQL, sin colecciones de `java.util`); la aplicación
   habla con el exterior por puertos (interfaces); los adaptadores (JDBC, BCrypt, web) los implementan.
5. **Seguridad en el backend:** `InterceptorRoles` revisa cada petición antes del controlador (lista blanca); contraseña
   y PIN solo como hash BCrypt.

**Números para la demo (caso 1):** 2.000.000 al 8 % → cuotas de 160.000 los días 10. Si el 10 ene paga 660.000:
160.000 a la cuota y 500.000 a capital; con capital vigente las siguientes cuotas bajan a 120.000.

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

---

## Hito 2: Dominio del rédito

### Qué se construyó

El corazón del negocio en Java puro (sin Spring, sin base de datos): un préstamo tipo **rédito** que
genera sus cuotas de interés a medida que pasa el tiempo, recibe pagos, los reparte y se cierra cuando
el capital llega a 0 *(desde el hito 3: cuando no queda capital, ni cuotas, ni multas por pagar)*.

| Clase | Paquete | Qué es |
|---|---|---|
| `Cuota` | `dominio.modelo` | Lo que se debe pagar en una fecha. Sabe cuánto le han cubierto y **calcula** su estado |
| `EstadoCuota` | `dominio.modelo` | `CON_TIEMPO`, `PAGADA`, `PAGADA_CON_RETRASO`, `MORA` |
| `BaseInteres` | `dominio.modelo` | `CAPITAL_INICIAL` (el interés nunca baja) o `CAPITAL_VIGENTE` (baja si se abona a capital) |
| `Contrato` | `dominio.modelo` | Las condiciones: capital, tasa, base, fecha de desembolso, frecuencia. Todo `final`, sin setters |
| `Prestamo` | `dominio.modelo` | Contrato + cronograma (`ListaDoble<Cuota>`) + pagos (`Pila<Pago>`) + capital pendiente *(desde el hito 4 también tiene `id`)* |
| `Pago` | `dominio.modelo` | Fecha, monto y cómo se repartió: `abonoACuotas` + `abonoACapital` *(el hito 3 agrega `abonoAMultas` y la marca de cancelación; el hito 5, las cuotas cubiertas y el saldo después)* |
| `Frecuencia` | `dominio.politicas` | Interfaz (patrón **Strategy**): calcula la fecha de vencimiento de la cuota número N |
| `FrecuenciaMensual` | `dominio.politicas` | Cobro mensual en un día fijo; si el mes no tiene ese día, el último día del mes |

Pruebas: `CuotaTest` (9), `FrecuenciaMensualTest` (7), `PrestamoTest` (21). Total del proyecto: 82.

### Cómo funciona el rédito, paso a paso

1. **Cuota = base × tasa / 100**, redondeada al peso con `HALF_UP` (1.500,50 → 1.501; 37.037,01 → 37.037).
   La base es el capital prestado (`CAPITAL_INICIAL`) o el capital pendiente en ese momento (`CAPITAL_VIGENTE`).
2. **El cronograma crece solo**: al crear el préstamo nace la cuota 1. La siguiente nace **el día después**
   de que vence la anterior. Siempre hay una sola cuota "actual": la última de la lista.
3. **Los pagos son al préstamo**, no a una cuota. `registrarPago` reparte así:
   cuotas vencidas (de la más vieja a la más nueva) → cuota actual → **lo que sobra va a capital**.
4. **Cierre**: cuando el capital pendiente llega a 0, el préstamo se cierra, no genera más cuotas y no recibe pagos.
   *(Cambió en el hito 3: con la cuota final que incluye el capital, cerrado = sin capital, cuotas ni multas pendientes.)*

### Decisiones importantes (y por qué)

**¿Por qué la cuota siguiente nace el día *después* del vencimiento y no el mismo día?**
Por el caso 2. El 10 de enero el cliente paga 660.000. Si la cuota de febrero ya existiera ese día:
- el pago le daría 160.000 a esa cuota y solo 340.000 irían a capital (no 500.000);
- con `CAPITAL_VIGENTE`, esa cuota ya se habría calculado con 2.000.000 y valdría 160.000, no 120.000.
Generándola el 11 de enero, el abono a capital del 10 ya cuenta para el interés del siguiente periodo.
Además encaja con `Cuota.estado`: el día del vencimiento la cuota todavía está `CON_TIEMPO`.

**¿Por qué cada fecha se calcula desde el desembolso y no sumando un mes a la anterior?**
Con cobro el día 30: si se sumara un mes a la anterior, 28 feb + 1 mes = 28 mar, y el préstamo quedaría
cobrando el 28 para siempre. Calculando "mes del desembolso + N meses, día 30 o el último del mes",
marzo vuelve al 30. Se usa `YearMonth` de `java.time` y su `lengthOfMonth()` (ya sabe de años bisiestos).

**¿Por qué el estado de la cuota se calcula y no se guarda?**
Porque depende del día en que se consulta: una cuota sin pagar pasa de `CON_TIEMPO` a `MORA` sola,
al pasar la fecha. Si se guardara, habría que actualizarla todos los días y podría quedar desactualizada.
Lo que sí se guarda es `fechaCubierta`, el día en que se completó, porque es un hecho que no cambia.

**¿Por qué el "hoy" llega como parámetro?**
`estado(hoy)`, `actualizarCronograma(hoy)` y `registrarPago(fecha, ...)` reciben la fecha. Así el dominio
no depende del reloj del computador, las pruebas usan las fechas exactas de los casos, y en la demo se
podrá simular otra fecha. *(Quedó así en el hito 4: `ConfiguracionReloj`, en `adaptadores/configuracion`, crea el
`Clock` de `America/Bogota`; los casos de uso lo reciben y le piden la fecha de hoy.)*

**¿Por qué se valida todo antes de repartir el pago?**
Si un pago se rechazara a mitad del reparto, algunas cuotas ya estarían cubiertas y el préstamo quedaría
inconsistente. Por eso primero se valida (préstamo abierto, monto > 0, fecha no anterior al desembolso ni
al último pago, monto ≤ saldo total) y solo después se toca algo.

**¿Por qué se rechazan pagos con fecha anterior al último pago?**
Porque "un préstamo es su contrato más sus pagos en orden": aplicar los mismos pagos en el mismo orden
debe dar siempre el mismo estado. Eso es lo que permitirá **deshacer** en el hito 3: sacar el último pago
de la pila y volver a aplicar los demás desde el contrato.

**Dinero con `BigDecimal` y `compareTo`.** `double` no representa exactamente valores como 0,1, y en dinero
eso produce errores de centavos. Para comparar montos se usa `compareTo`, no `equals`, porque `equals`
también compara la escala: `160000` y `160000.00` son distintos para `equals` e iguales para `compareTo`.

### Estructuras de datos en este hito

| Dónde | Estructura | Por qué |
|---|---|---|
| `Prestamo.cronograma` | `ListaDoble<Cuota>` | Crece por el final una cuota a la vez (`agregarAlFinal`, O(1)); se lee la última cuota para saber si ya venció (`ultimo`, O(1)); se recorre en orden para repartir pagos |
| `Prestamo.pagos` | `Pila<Pago>` | El último pago es el que se deshace (LIFO); `verTope` da la fecha del último pago para validar el orden, en O(1) |

### Complejidad de las operaciones del préstamo

Siendo *n* el número de cuotas del cronograma y *k* las cuotas nuevas que hay que generar:

| Operación | Complejidad | Por qué |
|---|---|---|
| `Frecuencia.fechaVencimiento` | O(1) | cálculo directo con `YearMonth`, sin recorrer meses |
| `actualizarCronograma(hoy)` | O(k) | cada cuota nueva es un `ultimo()` y un `agregarAlFinal()`, ambos O(1) |
| `saldoTotal()` | O(n) | suma el saldo de todas las cuotas *(desde el hito 3 es `saldoTotal(hoy)` y suma también las multas)* |
| `registrarPago` | O(n + k) | actualiza el cronograma, calcula el saldo y recorre las cuotas una vez |
| `estaCerrado()` | O(1) | compara el capital con 0 *(desde el hito 3 es O(n): revisa también cuotas y multas)* |
| `Cuota.estado`, `Cuota.cubrir` | O(1) | comparaciones y una suma |

En un rédito de máximo 24 meses, *n* ≤ 24 con frecuencia mensual: recorrer el cronograma es inmediato.

### Casos de prueba obligatorios y dónde se verifican

| Caso | Qué se comprueba | Prueba |
|---|---|---|
| 1 | 2.000.000 al 8 %: cuotas 10 dic, 10 ene, 10 feb, 10 mar de 160.000 | `PrestamoTest.caso1CuatroCuotasDe160000LosDias10` |
| 2 | Paga 660.000 el 10 ene: 160.000 a cuota y 500.000 a capital; con base inicial siguen en 160.000, con base vigente bajan a 120.000 | `PrestamoTest.caso2...` (3 pruebas) |
| 3 | Paga 100.000 el 10 feb: quedan 60.000 pendientes y la cuota pasa a `MORA` el 11 feb | `PrestamoTest.caso3...` |
| 4 | 8.000.000 al 5 %, cobro día 30: cuota de 400.000, febrero vence el 28 | `PrestamoTest.caso4...` y `FrecuenciaMensualTest` |
| 5 | Estados de la cuota de 160.000 que vence el 10 feb | `CuotaTest` (4 pruebas del caso 5) |

### Preguntas que podría hacer el profesor

**1. Si el cliente paga más de lo que debe en cuotas, ¿qué pasa con el excedente, y cómo afecta las cuotas siguientes?**

El préstamo recorre el cronograma de la cuota más vieja a la actual y a cada una le da lo que le falte,
sin pasarse (`restante.min(saldoPendiente)`). Lo que sobra al final baja el capital pendiente. Si la base
es `CAPITAL_INICIAL`, las cuotas siguientes no cambian, porque siempre se calculan sobre el capital prestado.
Si es `CAPITAL_VIGENTE`, la siguiente cuota que nazca se calcula sobre el capital nuevo y por eso baja:
en el caso 2, de 160.000 a 120.000 (1.500.000 × 8 %). Las cuotas que ya existían no se recalculan.

**2. ¿Por qué el cronograma es una lista doble y no se genera completo al crear el préstamo?**

Porque en el rédito no se sabe cuántas cuotas habrá ni cuánto valdrán: depende de cuándo y cuánto abone
el cliente a capital. Con `CAPITAL_VIGENTE`, el valor de cada cuota se conoce recién cuando nace. Por eso
el cronograma crece una cuota a la vez por el final. La lista doble lo hace en O(1) con `agregarAlFinal`,
y con `ultimo()` se consulta en O(1) si la última cuota ya venció para generar la siguiente.

**3. ¿Qué pasa con el estado de una cuota que se pagó a tiempo, si la consulto un mes después?**

Sigue saliendo `PAGADA`. El estado de una cuota cubierta se decide comparando `fechaCubierta` (el día en
que se completó, guardado al momento del pago) con su fecha de vencimiento, no con la fecha de consulta.
Solo las cuotas **sin cubrir** comparan el "hoy" con el vencimiento: hasta ese día están `CON_TIEMPO` y
desde el día siguiente en `MORA`. Hay una prueba exactamente para esto en `CuotaTest`.

---

## Hito 3: Políticas del contrato

### Qué se construyó

Todas las condiciones que el prestamista elige al crear el préstamo, y las acciones que dependen de ellas:
mora con días de gracia, condonación, dos órdenes de imputación, deshacer, plazo máximo y cancelación anticipada.

| Clase | Paquete | Tipo | Qué decide |
|---|---|---|---|
| `Frecuencia` + `FrecuenciaMensual`, `FrecuenciaQuincenal`, `FrecuenciaSemanal` | `politicas` | Strategy | Fecha de cada cuota y cuántas caben en 24 meses (24 / 48 / 104) |
| `PoliticaMora` + `SinMulta`, `MultaFijaDiaria`, `PorcentajeUnico`, `PorcentajeDiario` | `politicas` | Strategy | Cuánto vale la multa según los días con multa |
| `Calendario` | `politicas` | enum | `TODOS_LOS_DIAS` o `SIN_DOMINGOS` (domingo → lunes) |
| `OrdenImputacion` | `politicas` | enum | `MULTAS_PRIMERO` o `CUOTAS_PRIMERO` |
| `AlVencerPlazo` | `politicas` | enum | `TRATAR_COMO_MORA` o `SEGUIR_COBRANDO` |
| `InteresCancelacion` | `politicas` | enum | `PERIODO_COMPLETO` o `POR_DIAS` |
| `Contrato.Borrador` | `modelo` | Builder | Arma el contrato con los valores por defecto y lo "firma" |

Pruebas al cerrar el hito: 163 en total (81 nuevas en este hito).

### Strategy o enum: cómo se decidió

- **Strategy (interfaz + una clase por opción)** cuando cada opción trae **sus propios datos y su propio cálculo**:
  `MultaFijaDiaria` guarda el valor diario; `PorcentajeDiario`, el porcentaje; `FrecuenciaMensual`, el día del mes;
  `FrecuenciaSemanal`, el día de la semana. El préstamo solo conoce la interfaz: agregar una opción nueva no cambia `Prestamo`.
- **enum** cuando la opción es solo una elección entre dos caminos, sin datos. Un enum también puede tener
  comportamiento: `Calendario.ajustar(fecha)`.

### El contrato se crea con un borrador (patrón Builder)

```java
Contrato contrato = Contrato.borrador(capital, tasa, desembolso)   // lo que no tiene valor por defecto
        .base(BaseInteres.CAPITAL_VIGENTE)                          // se cambia solo lo que se quiera
        .mora(new MultaFijaDiaria(new BigDecimal("2000")))
        .firmar();                                                  // aquí se valida y nace el contrato
```

Valores por defecto (todos en `Contrato.Borrador`): base `CAPITAL_INICIAL`, mensual el día del desembolso,
`TODOS_LOS_DIAS`, `SinMulta`, 2 días de gracia, `MULTAS_PRIMERO`, `TRATAR_COMO_MORA`, `PERIODO_COMPLETO`.
El constructor de `Contrato` es privado: la única forma de crearlo es firmar un borrador. El contrato firmado
tiene todo `final`; el borrador sí cambia, pero es solo un formulario.

### Cada pieza en pocas palabras

**Frecuencias.** Todas calculan la fecha de la cuota N desde el desembolso, en O(1), sin recorrer fechas.
- Quincenal: cada mes tiene dos cortes, numerados 0 (día 15) y 1 (último día); la cuota N es el corte "primero + N − 1".
- Semanal: la primera es el primer día de cobro que esté al menos 7 días después del desembolso; luego, +7 días.
- Quincenal y semanal nunca dan un primer periodo más corto que uno completo.

**Calendario.** `Contrato.fechaVencimiento(n)` = `calendario.ajustar(frecuencia.fechaVencimiento(desembolso, n))`.
Mover un domingo al lunes no corre las cuotas siguientes, porque cada fecha se calcula por su número.

**Mora.** La multa **se calcula, no se guarda**:
1. `Cuota.diasDeAtraso(hoy)`: del vencimiento hasta el día en que se cubrió (o hasta hoy si no está cubierta).
2. Días con multa = días de atraso − días de gracia (nunca negativo). Esto lo hace `Contrato.multa(cuota, hoy)`,
   una sola vez para las 4 políticas.
3. `PoliticaMora.multa(valorCuota, diasConMulta)`.

Ejemplo: cuota de 160.000 que vence el 10 feb, 2 días de gracia, consulta el 15 feb (3 días con multa):
fija de 2.000 → 6.000; 10 % único → 16.000; 1 % diario → 4.800. Si la cuota se cubre el 14 feb, la multa queda
congelada en 2 días. La cuota guarda solo `multaPagada`; lo que falta es multa calculada − pagada.

**Imputación.** `registrarPago` reparte con dos métodos, `pagarMultas` y `pagarCuotas` (ambos de la cuota más vieja
a la más nueva). El orden del contrato solo decide cuál se llama primero. Lo que sobra va a capital.
Con 170.000 el 15 dic (multa de 6.000): `MULTAS_PRIMERO` → 6.000 multa, 160.000 cuota 1, 4.000 cuota 2;
`CUOTAS_PRIMERO` → 160.000 cuota 1, 10.000 cuota 2, multa pendiente.

**Deshacer.** `deshacerUltimoPago()` desapila el último pago (O(1)) y llama a `recalcular()`:
1. pasa los pagos de la pila a una `ListaDoble` con `agregarAlInicio` (eso invierte el orden: del más viejo al más nuevo);
2. deja el préstamo como el día del desembolso;
3. vuelve a llamar a `registrarPago` (o `cancelar`) con cada pago.

No se "resta" el pago: un pago pudo pagar multas, cerrar cuotas, bajar capital y cambiar el valor de cuotas
posteriores (capital vigente). Volver a aplicar los demás siempre da el resultado correcto.

**Condonar.** `condonarMulta(numeroCuota, hoy)` fija la multa de esa cuota en lo que ya estaba pagado: lo pagado
se respeta y lo que faltaba se anula. Se anota en una `TablaHash<Integer, BigDecimal>` (número de cuota → lo pagado),
y cada cuota que nace en un recálculo busca su número en la tabla (O(1)) y nace condonada si estaba.

**Plazo máximo.** La cuota número `cuotasEnPlazoMaximo()` incluye, además del interés, todo el capital pendiente
(`Cuota.capitalIncluido`), y el capital del préstamo pasa a 0. Dentro de la cuota se paga primero el interés.
Al vencer sin pagar: `TRATAR_COMO_MORA` no genera más cuotas (la última queda en mora); `SEGUIR_COBRANDO` genera
una cuota de interés por periodo mientras quede capital sin pagar (con capital vigente, sobre ese capital).

**Cancelación anticipada.** `valorParaCancelar(hoy)` = cuotas vencidas + multas + capital + cuota en curso.
Con `POR_DIAS` y antes del vencimiento, la cuota en curso se cobra solo por los días corridos:
interés × días corridos / días del periodo (lo ya pagado no se devuelve). Caso 1 al 25 nov (15 de 30 días):
2.160.000 con periodo completo, 2.080.000 por días. `cancelar(fecha)` rebaja la cuota y paga exacto; el pago queda
marcado `cancelacion` para poder reconstruir el préstamo desde sus pagos.

### Cambios a lo construido en el hito 2

- **Cierre:** antes era "capital pendiente = 0". Ahora es "no queda capital, ni cuotas sin cubrir, ni multas".
  Hizo falta porque después de la cuota final el capital del préstamo es 0 pero la deuda está dentro de la cuota,
  y con `CUOTAS_PRIMERO` la cuota final puede quedar pagada con su multa pendiente.
- **`saldoTotal(hoy)`** ahora recibe la fecha, porque incluye las multas, que dependen del día.
- **Capital vigente:** el interés se calcula sobre `capitalAdeudado()` (pendiente + capital sin pagar de la cuota final).
  Antes del plazo es lo mismo que el capital pendiente.
- **El contrato** ya no tiene constructor público: se crea con `Contrato.borrador(...).firmar()`.
- **`Pago`** tiene tres partes (`abonoAMultas`, `abonoACuotas`, `abonoACapital`) y la marca `cancelacion`.

### Estructuras de datos en este hito

| Dónde | Estructura | Por qué |
|---|---|---|
| Deshacer | `Pila<Pago>` | Siempre se anula el último pago: `desapilar` lo da en O(1) |
| Recalcular | `ListaDoble<Pago>` | `agregarAlInicio` (O(1)) invierte el orden de la pila para aplicar los pagos del más viejo al más nuevo |
| Condonaciones | `TablaHash<Integer, BigDecimal>` | Cada cuota que nace pregunta "¿tengo condonación?" en O(1) |
| Cronograma | `ListaDoble<Cuota>` | Se recorre en orden para repartir multas y cuotas; `ultimo()` da la cuota en curso en O(1) |

### Complejidad

Con *n* cuotas en el cronograma y *p* pagos:

| Operación | Complejidad | Por qué |
|---|---|---|
| `Frecuencia.fechaVencimiento`, `Calendario.ajustar` | O(1) | cálculo directo |
| `PoliticaMora.multa`, `Contrato.multa`, `Cuota.diasDeAtraso` | O(1) | aritmética |
| `saldoMulta` | O(1) | multa calculada − pagada |
| `saldoTotal(hoy)`, `capitalAdeudado()`, `estaCerrado()` | O(n) | recorren el cronograma |
| `registrarPago`, `cancelar`, `valorParaCancelar` | O(n) | recorren el cronograma un número fijo de veces |
| `condonarMulta` | O(n) | `obtener(i)` de la lista camina hasta la cuota; guardar en la tabla es O(1) |
| `deshacerUltimoPago` | O(p × n) | vuelve a aplicar cada pago |

Con máximo 24 cuotas mensuales (104 semanales) y pocos pagos, todo es inmediato.

### Preguntas que podría hacer el profesor

**1. ¿Dónde usaron el patrón Strategy y qué ganaron con él?**

En `PoliticaMora` (4 opciones) y `Frecuencia` (3 opciones). El contrato guarda un objeto de la interfaz y el
préstamo solo llama `politicaMora.multa(...)` o `frecuencia.fechaVencimiento(...)`, sin saber cuál opción es.
Lo demostramos en el commit de las frecuencias quincenal y semanal: se agregaron dos opciones y `Prestamo` no
cambió ni una línea. Para elecciones sin datos (calendario, orden de imputación, qué hacer al vencer el plazo,
interés al cancelar) usamos enums, porque una interfaz con dos clases vacías sería más código sin ganar nada.

**2. ¿Cómo funciona deshacer y por qué no simplemente "restan" el pago?**

Se saca el último pago de la pila (O(1), porque deshacer siempre afecta al último) y se recalcula: el préstamo
vuelve al estado del desembolso y se aplican de nuevo los pagos que quedan, del más viejo al más nuevo. Para
eso se invierte la pila pasándola a una lista doble con `agregarAlInicio`. No se resta porque un pago pudo haber
pagado multas, cubierto cuotas, bajado el capital y, con capital vigente, cambiado el valor de cuotas que nacieron
después. Revertir cada efecto a mano tiene muchos casos especiales; volver a aplicar los pagos da siempre el mismo
resultado, porque el préstamo es su contrato más sus pagos en orden.

**3. Si condono una multa y después deshago un pago, ¿se pierde la condonación?**

No. Las condonaciones se guardan aparte, en una tabla hash de número de cuota → lo que ya estaba pagado al condonar.
Al recalcular, cada cuota que nace busca su número en la tabla (O(1)) y, si estaba, nace con la multa fijada en ese
valor. Eso da el mismo resultado que si la condonación hubiera ocurrido en su momento. Un caso sutil: si el pago que
se deshace es uno que había abonado a la multa antes de condonarla, ese abono vuelve a deberse, porque la condonación
solo perdonó lo que faltaba, no lo que ya estaba pagado.

---

## Hito 4: Persistencia y casos de uso

### Qué se construyó

La aplicación ya guarda y lee sus datos de una base de datos H2 en archivo, y tiene los casos de uso que usará la web.

```
dominio            ← no cambió su forma: sigue sin Spring ni SQL
aplicacion
├── puertos        RepositorioClientes, RepositorioPrestamos, CifradorPin   (interfaces)
└── casos de uso   DirectorioClientes, CrearCliente, CrearPrestamo, ConsultarPrestamos,
                   RegistrarPago, DeshacerUltimoPago, CondonarMulta, CancelarPrestamo
adaptadores
├── persistencia   RepositorioClientesJdbc, RepositorioPrestamosJdbc, ConversorContrato
├── seguridad      CifradorPinBCrypt
└── configuracion  ConfiguracionReloj (Clock), ConfiguracionCasosDeUso (conecta todo)
```

Migraciones Flyway: `V1__crea_esquema.sql` (tablas) y `V2__datos_de_prueba.sql` (4 clientes y 5 préstamos inventados).
Pruebas al cerrar el hito: 235 (72 nuevas).

### La idea central: la base de datos guarda lo que pasó, el dominio calcula

Se guardan solo cuatro cosas: **clientes**, el **contrato** de cada préstamo, sus **pagos** en orden y las
**condonaciones**. No hay tabla de cuotas ni columnas de saldo o estado.

Leer un préstamo es reconstruirlo, siempre igual:
1. leer la fila del préstamo y armar el `Contrato` con el borrador;
2. `new Prestamo(id, cedula, contrato)`;
3. `restaurarCondonacion(...)` por cada condonación guardada;
4. `registrarPago(...)` o `cancelar(...)` por cada pago, en orden de `id`.

Ventajas: la base de datos nunca puede contradecir al cálculo (solo hay una fuente de verdad), deshacer es borrar
una fila, y las reglas de negocio viven en un solo lugar (el dominio, que ya está probado).

### Tablas

| Tabla | Qué guarda | Detalles |
|---|---|---|
| `cliente` | datos y hash del PIN | cédula como llave primaria |
| `prestamo` | las 11 condiciones del contrato | `frecuencia` + `dia_cobro`; `politica_mora` + `valor_mora`; enums con `CHECK ... IN` |
| `pago` | fecha, monto, si fue cancelación, fecha-hora de registro | el `id` crece con cada pago: da el orden y es el número de recibo |
| `condonacion` | número de cuota y en cuánto quedó fijada la multa | llave (préstamo, cuota): no se condona dos veces |

SQL estándar (`GENERATED ALWAYS AS IDENTITY`, `DATE '...'`) y H2 en modo PostgreSQL: para migrar a PostgreSQL se agregan
al `pom.xml` el driver y el módulo de Flyway para PostgreSQL, y se cambian las variables `DB_URL`, `DB_USUARIO` y `DB_CLAVE`.
*(Corregido en el repaso final: antes decía que bastaba con las variables.)*

### Arquitectura hexagonal: puertos y adaptadores

- Un **puerto** es una interfaz en `aplicacion/puertos` que dice *qué* necesita la aplicación (guardar un pago,
  cifrar un PIN) sin decir *cómo*.
- Un **adaptador** en `adaptadores` lo implementa con una tecnología concreta (JDBC, BCrypt).
- Los casos de uso son clases de **Java puro**, sin anotaciones de Spring. Spring los crea en un solo lugar:
  `ConfiguracionCasosDeUso`. Gracias a eso se prueban con repositorios falsos en milisegundos.
- Las excepciones de Spring no cruzan al centro: `DuplicateKeyException` se traduce a `IllegalArgumentException`.

### Cómo trabaja cada caso de uso

Todos siguen el mismo molde: **cargar → el dominio actúa y valida → guardar solo lo nuevo**.

| Caso de uso | Lo que guarda |
|---|---|
| `RegistrarPago` | una fila en `pago` (fecha = hoy del reloj) |
| `DeshacerUltimoPago` | borra la fila de pago con mayor `id` de ese préstamo |
| `CondonarMulta` | una fila en `condonacion` |
| `CancelarPrestamo` | una fila en `pago` marcada como cancelación |
| `CrearPrestamo` | una fila en `prestamo`, tras validar monto mínimo y cupo |
| `CrearCliente` | una fila en `cliente` con el PIN cifrado, y lo agrega a la tabla hash |

Como el dominio actúa **antes** de guardar, si rechaza la operación la excepción sale antes del `INSERT` y la base
de datos no cambia.

**Cupo:** deuda vigente = capital adeudado de los préstamos activos del cliente (sin intereses ni multas). Si deuda +
préstamo nuevo supera el cupo y el prestamista no ha confirmado, `CrearPrestamo` lanza `CupoExcedidoException` con los
tres valores, para que la pantalla pida la confirmación.

**Dinero sin centavos:** capital y pagos se rechazan si traen centavos; el dinero va al peso y las columnas no tienen decimales.

### El reloj

`ConfiguracionReloj` crea el único `Clock` del sistema, en `America/Bogota`. Si se arranca con
`FECHA_SIMULADA=2027-01-20`, el reloj se corre los días necesarios para que "hoy" sea esa fecha (la hora sigue siendo
la real). Así en la demo se puede mostrar cómo crece la mora sin tocar el código ni los datos.

### Estructuras de datos en este hito

| Dónde | Estructura | Por qué |
|---|---|---|
| `DirectorioClientes` | `TablaHash<String, Cliente>` | Búsqueda por cédula (buscador, cédula repetida, login del hito 6) en O(1) sin ir a la base de datos; se carga una vez al arrancar |
| Repositorios | `ListaDoble<T>` | Cada fila leída se agrega con `agregarAlFinal`: ni siquiera el adaptador usa colecciones de `java.util` |
| Reconstrucción | `Pila`, `ListaDoble`, `TablaHash` del dominio | Repetir los pagos usa las mismas estructuras de los hitos 2 y 3 |

### Complejidad

Con *c* clientes, *p* pagos de un préstamo, *n* cuotas y *P* préstamos:

| Operación | Complejidad | Por qué |
|---|---|---|
| `DirectorioClientes.buscar` | O(1) promedio | tabla hash |
| Cargar el directorio al arrancar | O(c) | una consulta y un `guardar` por cliente |
| Leer un préstamo | O(p × n) + 3 consultas | repetir cada pago recorre el cronograma |
| Listar todos | O(P × p × n) + 2 consultas por préstamo | se reconstruye cada uno |
| Pagar, deshacer, condonar, cancelar | lo de leer + 1 `INSERT` o `DELETE` | |

Para un solo prestamista (decenas o cientos de préstamos de máximo 24 cuotas) todo esto es instantáneo.

### Datos de prueba (PIN 1234 para todos)

| Cédula | Cliente | Préstamo | Estado a mediados de noviembre de 2026 |
|---|---|---|---|
| 1000000001 | Laura Gómez | 2.000.000 al 8 % mensual | al día |
| 1000000001 | Laura Gómez | 300.000 al 3 % semanal (martes) | al día |
| 1000000002 | Carlos Mejía | 1.500.000 al 10 % mensual, multa 2.000/día | en mora (cuotas 5 oct y 5 nov) |
| 1000000003 | Marta Ospina | 5.000.000 al 3 % quincenal, capital vigente, sin domingos | abonó a capital; cuota del 15 nov pasa al 16 |
| 1000000004 | Jorge Castaño | 1.000.000 al 10 % mensual, interés por días | cerrado por cancelación anticipada |

`DatosDePruebaTest` comprueba estos estados a una fecha fija.

### Preguntas que podría hacer el profesor

**1. ¿Dónde guardan las cuotas y el saldo de cada préstamo?**

No se guardan. La base de datos guarda solo lo que pasó: el contrato, los pagos en orden y las condonaciones. Al leer
un préstamo, el repositorio lo crea con su contrato, restaura las condonaciones y repite los pagos; el dominio vuelve a
generar las cuotas en la lista doble y a calcular saldos, estados y multas. Así no hay dos copias de la verdad que puedan
quedar distintas, y deshacer un pago es simplemente borrar su fila.

**2. ¿Para qué sirven los puertos si solo tienen una implementación?**

En realidad tienen dos: la de JDBC, que usa la aplicación, y una en memoria (hecha con nuestras estructuras) que usan las
pruebas de los casos de uso. El puerto hace que la aplicación dependa de una interfaz y no de JDBC ni de Spring: se puede
probar sin base de datos, y cambiar H2 por PostgreSQL (o JDBC por otra cosa) solo toca el adaptador.

**3. Si los clientes están en la base de datos, ¿por qué también están en una tabla hash?**

Porque buscar por cédula es la operación más frecuente (buscador, validar que no se repita, login del cliente) y la
tabla hash la resuelve en O(1) en memoria, con nuestra propia estructura. La base de datos es la que los guarda de forma
permanente; la tabla se llena una sola vez al arrancar (`listarTodos`) y se actualiza cada vez que se crea un cliente.
Sus métodos son `synchronized` para que dos peticiones web simultáneas no la dañen.

---

## Hito 5: Web del prestamista

### Qué se construyó

La interfaz web con Thymeleaf y un solo archivo de CSS propio (sin frameworks de frontend). Se arranca con
`./mvnw spring-boot:run` y se abre `http://localhost:8080`.

| Ruta | Pantalla | Estructura que se ve trabajando |
|---|---|---|
| `/` | Panel: agenda de cobros y lista de mora | `ColaPrioridad` (dos, con comparadores distintos) |
| `/clientes` | Lista y buscador por cédula | `TablaHash` del `DirectorioClientes` |
| `/clientes/nuevo` | Crear cliente | |
| `/clientes/{cedula}` | Ficha: datos, notas internas, deuda vs. cupo, préstamos | |
| `/prestamos/nuevo?cedula=` | Formulario con las 11 condiciones y confirmación de cupo | |
| `/prestamos/{id}` | Detalle: resumen, contrato, cronograma, pagos y acciones | `ListaDoble` (cronograma), `Pila` (pagos y deshacer), `TablaHash` (condonaciones) |
| `/prestamos/{id}/recibos/{n}` | Recibo imprimible con enlace de WhatsApp | |

Clases nuevas:
- Dominio: `Recibo`; `Pago` ahora recuerda las cuotas que cubrió y el saldo que dejó.
- Aplicación: `ArmarPanel` (con `Panel`, `CobroAgendado`, `PrestamoEnMora`), `GenerarRecibo`, `RegistroDePago`.
- Web (`adaptadores/web`): `PanelControlador`, `ClientesControlador`, `PrestamosControlador`,
  `DetallePrestamoControlador`, `ReciboControlador`, `FormularioCliente`, `FormularioPrestamo`, `Textos`, `ModeloComun`.

Pruebas al cerrar el hito: 285 (50 nuevas).

### El panel: dos colas de prioridad

`ArmarPanel` recorre los préstamos activos una sola vez y llena dos colas:

- **Agenda:** la próxima cuota sin cubrir (que aún no vence) de cada préstamo, si vence entre hoy y los próximos
  7 días. Comparador: fecha más cercana primero (y nombre si coinciden). Las de hoy salen resaltadas.
- **Mora:** los préstamos con alguna cuota vencida sin cubrir. Comparador: días de atraso de la cuota más vieja,
  **invertido** (`.reversed()`), para que el montículo, que es de mínimo, entregue primero al más atrasado.
  El total vencido suma lo que falta de esas cuotas y todas las multas pendientes.

La vista recorre cada cola con `th:each`. El iterador de `ColaPrioridad` extrae de una copia: las filas salen en
orden y la cola no se vacía.

**La agenda como tarjetas ("Esta semana").** La agenda sigue saliendo de la misma `ColaPrioridad` por fecha:
solo cambió cómo se dibuja. En vez de una fila de tabla, cada cobro es una tarjeta con:

- un calendario pequeño con el día de la semana (LUN, MAR, MIÉ...) o "HOY", y el número del día;
- el cliente y "Préstamo #4 · cuota 4", más " · semanal" o " · quincenal" si no es mensual;
- el valor, un botón de recordatorio por WhatsApp y un botón "Detalle".

Las tarjetas salen en el orden en que las entrega el iterador (la fecha más cercana primero). La vista no ordena
nada: si se ordenara en la plantilla, la cola de prioridad dejaría de tener sentido.

Piezas que intervienen:
- `CobroAgendado` (aplicación) ganó el campo `frecuencia`, que viene del contrato. Así la vista sabe si el
  préstamo es semanal o quincenal sin consultar otra vez el préstamo. El dominio no cambió.
- `Textos` (adaptador web) arma los textos de la tarjeta (`diaCalendario`, `detalleCobro`) y el enlace
  `https://wa.me/57<teléfono>?text=...` (`enlaceRecordatorio`). El texto va codificado para URL con `URLEncoder`,
  igual que el del recibo.
- "Detalle" lleva a `/prestamos/{id}?monto=<valor de la cuota>#registrar-pago`: el ancla baja directo al
  formulario y la plantilla del detalle escribe el monto con `th:value="${param.monto}"`. No hizo falta tocar
  el controlador.
- `AgendaWebTest` fija la fecha en el 16 nov 2026 y comprueba que la primera tarjeta es la de la fecha más cercana.

Pregunta posible: **¿Por qué las tarjetas salen en orden si un montículo no está ordenado por dentro?**
Porque `th:each` no recorre el arreglo del montículo: usa el iterador, que hace `extraer()` sobre una copia.
Cada extracción entrega el mínimo en O(log n), así que recorrer toda la agenda cuesta O(n log n) y la cola
original queda intacta.

### El recibo sin guardar recibos

Un recibo junta dos cosas que vienen de lugares distintos:
- **Del dominio:** el `Pago` (desglose, cuotas que quedaron cubiertas, saldo justo después). Se calcula al repartir
  el pago y, como el préstamo se reconstruye repitiendo los pagos en orden, siempre vuelve a salir igual.
- **De la base de datos:** el número (id de la fila del pago) y la fecha-hora de registro (`RegistroDePago`).

El pago número *n* del préstamo corresponde al registro número *n*: las dos listas están en el orden de los pagos.

**WhatsApp:** `https://wa.me/57<celular>?text=<recibo>`. El texto se codifica con `URLEncoder` (espacios → `%20`,
saltos de línea → `%0A`, tildes en UTF-8). El enlace se arma en el controlador: Thymeleaf no permite llamar beans
dentro de `th:href` por seguridad.

### Decisiones de la capa web

- **Controladores delgados:** solo leen la petición, llaman un caso de uso y eligen la vista. No tienen reglas de negocio.
- **POST → redirigir → GET:** después de crear, pagar, deshacer, condonar o cancelar se redirige. Si el usuario
  recarga, el navegador pide la página de nuevo en vez de repetir la acción.
- **Errores como mensajes:** cada acción atrapa los rechazos del dominio (`IllegalArgumentException`,
  `IllegalStateException`) y los muestra como aviso (mensaje flash). El usuario nunca ve una página de error.
- **Formularios:** `FormularioCliente` y `FormularioPrestamo` tienen setters porque Spring los llena con lo que llega
  del navegador; son de la capa web, no del dominio.
- **Valores por defecto en un solo lugar:** el formulario de préstamo no los repite; firma un contrato de ejemplo con
  `Contrato.Borrador` y copia sus condiciones.
- **Cupo:** si se supera, el formulario vuelve con la advertencia (cupo, lo que debe, lo que pide) y una casilla de
  confirmación. Con la casilla marcada, el préstamo se crea.
- **Las vistas llaman al dominio** directamente (`${cuota.estado(hoy)}`, `${prestamo.saldoMulta(cuota, hoy)}`), sin
  clases intermedias. `Textos` traduce las políticas a frases ("Mensual, el día 5", "$2.000 por día de atraso").
- **`hoy` en todas las vistas:** `ModeloComun` lo agrega desde el reloj; el menú muestra la fecha (real o simulada).
- **Formatos:** pesos como `$1.234.567` y fechas `dd/MM/yyyy`.
- **Imprimir:** el CSS oculta menú y botones al imprimir (`@media print`).

### Complejidad

| Operación | Complejidad | Por qué |
|---|---|---|
| Armar el panel | O(P × n + P log P) | recorre P préstamos con n cuotas e inserta en dos montículos |
| Mostrar cada lista del panel | O(P log P) | el iterador extrae de una copia |
| Buscar cliente por cédula | O(1) promedio | tabla hash |
| Detalle de un préstamo | O(p × n) | reconstruir = repetir p pagos sobre n cuotas |
| Recibo | O(p × n) | reconstruye el préstamo e invierte la pila de pagos (O(p)) |

### Cómo hacer la demo

1. `./mvnw spring-boot:run` → panel: Marta y Laura en la agenda ("Esta semana"); Carlos en mora (con multas).
2. Clientes → buscar `1000000002` → ficha de Carlos con sus notas internas.
3. Préstamo de Carlos → cronograma con cuotas "En mora" y multas → **Condonar** la multa de una cuota.
4. **Registrar pago** → recibo → **Enviar por WhatsApp** (abre el mensaje ya escrito).
5. Volver al préstamo → **Deshacer el último pago** → todo vuelve como estaba (la pila en acción).
6. Crear un préstamo grande a Laura → aparece la advertencia de cupo → marcar la casilla → se crea.
7. Para mostrar cómo crece la mora: `FECHA_SIMULADA=2027-01-20 ./mvnw spring-boot:run`.

### Preguntas que podría hacer el profesor

**1. ¿Cómo ordenan la lista de mora para que salga primero el más atrasado, si su cola de prioridad es de mínimo?**

La `ColaPrioridad` no sabe qué es un atraso: el orden lo decide el `Comparator` que recibe al crearla. Para la mora se
le pasa `Comparator.comparingInt(PrestamoEnMora::diasDeAtraso).reversed()`. Con el comparador invertido, el "menor"
para el montículo es el de más días de atraso, y por eso sale primero. La agenda usa la misma clase con un comparador
por fecha. Insertar cuesta O(log n) y mostrar la lista completa O(n log n), porque el iterador extrae de una copia sin
vaciar la cola.

**2. Si no guardan los recibos, ¿cómo pueden reimprimir el recibo de un pago de hace tres meses?**

El préstamo se reconstruye repitiendo sus pagos en orden, y al repetir cada pago el dominio vuelve a calcular su
desglose, qué cuotas cubrió y cuánto quedó debiendo, exactamente como el día en que se registró. La base de datos aporta
lo que el dominio no sabe: el número del recibo (id del pago) y la hora de registro. Se juntan por posición: el pago
número n con el registro número n.

**3. ¿Qué pasa si el usuario recarga la página justo después de registrar un pago? ¿Se registra dos veces?**

No. Después de cualquier POST el controlador responde con una redirección (patrón POST → redirigir → GET): el navegador
termina en la página del recibo, que es un GET. Recargar vuelve a pedir el recibo, no repite el pago. Si de todas formas
se registrara un pago por error, está el botón de deshacer el último pago, que lo saca de la pila y recalcula el préstamo.

---

## Hito 6: Login, roles y portal del cliente

### Qué se construyó

| Pieza | Clases | Qué hace |
|---|---|---|
| Iniciar sesión | `IniciarSesion`, `UsuarioSesion`, `Rol` | Un solo método, `entrar(identificador, clave)`, que deduce el rol: si es el usuario del prestamista verifica su contraseña; si no, busca la cédula en la `TablaHash` y verifica el PIN |
| Sesión HTTP | `SesionWeb`, `LoginControlador`, `login.html` | Guarda quién entró; al entrar descarta la sesión anterior; salir la cierra |
| Protección en el backend | `InterceptorRoles`, `ConfiguracionWeb`, `error/403.html` | Revisa cada petición antes del controlador (lista blanca) |
| Portal del cliente | `PortalClienteControlador`, `mis-prestamos.html`, `mi-prestamo.html` | Solo lectura: sus préstamos, progreso, próximo cobro, valor para estar al día, recibos |
| Simulador | `SimularAbono`, `ResultadoSimulacion` | "Si abono $X hoy": reparto, nuevo capital y, con capital vigente, nuevo interés |
| Cálculos del dominio | `Prestamo.valorParaEstarAlDia`, `proximaCuotaPendiente`, `porcentajeDeCapitalPagado`, `interesProximaCuota` | Lo que muestra el portal |

Pruebas al cerrar el hito: 327 (42 nuevas). *(330 después de unificar el login en un solo formulario.)*

### Usuarios para entrar

| Rol | Cómo entra | Datos de prueba |
|---|---|---|
| Prestamista | usuario + contraseña | `prestamista` / `cartera2026` |
| Cliente | cédula + PIN | `1000000001` a `1000000004`, PIN `1234` |

### Un solo formulario: el sistema deduce el rol

*(Al principio había dos formularios, "Soy cliente" y "Soy el prestamista", con dos rutas. Se unificó después del hito 6.)*

`login.html` tiene un solo formulario, "Cédula o usuario" y "Contraseña", que envía `POST /login`. El campo de contraseña
sirve para la contraseña del prestamista y para el PIN del cliente. `IniciarSesion.entrar(identificador, clave)` decide:

1. Si el identificador es el usuario del prestamista (el de `application.properties`), verifica la contraseña con BCrypt
   → rol `PRESTAMISTA`.
2. Si no, lo busca como cédula en la `TablaHash` del `DirectorioClientes` (O(1)) y verifica el PIN con BCrypt → rol `CLIENTE`.
3. Si nada coincide → "Usuario o clave incorrectos" (el mismo mensaje en todos los casos).

Para que el paso 1 nunca se confunda con una cédula, **el usuario del prestamista no puede ser solo números**: el
constructor de `IniciarSesion` lo valida y, si lo es, lanza una excepción con el motivo. Como ese constructor corre al
arrancar, una configuración así impide que la aplicación arranque.

`InterceptorRoles` no cambió: decide con el rol guardado en la sesión, no con la ruta por la que se entró. Solo se quitó
de `ConfiguracionWeb` la exclusión `/login/**`, que ya no tenía ninguna ruta.

### Contraseñas y PIN: solo hashes BCrypt

- La contraseña del prestamista no está escrita en ninguna parte: en `application.properties` solo está su hash BCrypt
  (`app.prestamista.clave-cifrada`). Se cambia con las variables `PRESTAMISTA_USUARIO` y `PRESTAMISTA_CLAVE_CIFRADA`.
- Los PIN de los clientes se guardan en la base de datos solo como hash.
- BCrypt es de una sola vía (no se puede sacar la clave del hash, solo comprobar si coincide), usa una "sal"
  aleatoria (el mismo PIN da un hash distinto cada vez) y es lento a propósito (adivinar por fuerza bruta es costoso).
- Si algo no coincide, el mensaje es siempre "Usuario o clave incorrectos": no revela si existe la cédula o el usuario.
- Se usa solo `spring-security-crypto` (la librería de BCrypt), no Spring Security completo.

### La sesión

- Al entrar se guarda un `UsuarioSesion` (rol, cédula, nombre) en la sesión HTTP.
- **Fijación de sesión:** al entrar se invalida la sesión anterior y se crea una nueva, para que nadie pueda "plantar"
  un identificador de sesión antes del login y usarlo después.
- **Solo cookie:** `server.servlet.session.tracking-modes=cookie`. Sin esto, Tomcat pegaba `;jsessionid=...` a la
  dirección en la primera respuesta: el identificador se filtraba al copiar enlaces y, además, el interceptor bloqueaba
  por error al cliente recién entrado (`"/mis-prestamos;jsessionid=…"` no es igual a `"/mis-prestamos"`). Lo encontramos
  probando la aplicación real, no en las pruebas automáticas.

### El interceptor de roles (lo más importante del hito)

`InterceptorRoles` implementa `HandlerInterceptor`. Su `preHandle` corre **antes** de cada controlador; si devuelve
`false`, la petición no llega a él. Está registrado para `/**` excepto `/login`, `/css` y `/error`.

| Quién | Pasa | Si no |
|---|---|---|
| Sin sesión | — | redirige a `/login` |
| Prestamista | todo | 403 en `/mis-prestamos` |
| Cliente | GET `/mis-prestamos`; GET `/mis-prestamos/{id}/...` **solo si el préstamo es suyo**; POST `/salir` | **403** |

- **Por qué en el backend:** ocultar botones no protege nada. Un cliente puede escribir `/prestamos/3` o mandar un POST
  con `curl`. Las pruebas del interceptor envían esas peticiones directamente y verifican que la respuesta sea 403 y que
  no se haya guardado nada.
- **Por qué lista blanca:** se permite solo lo que está en la lista. Una pantalla nueva del prestamista queda bloqueada
  para el cliente automáticamente, sin tener que acordarse de prohibirla.
- **Préstamos ajenos:** para `/mis-prestamos/{id}`, el interceptor lee el préstamo y compara su cédula con la de la sesión.
  Si no existe también responde 403, para no revelar qué números de préstamo existen.

### El portal del cliente

- **Barra de progreso** = capital devuelto ÷ capital prestado. En rédito las cuotas son solo interés; el avance real es
  el capital que se ha devuelto. Un préstamo cerrado está al 100 %.
- **Próxima fecha de cobro** = la primera cuota sin cubrir.
- **Para estar al día hoy** = cuotas que vencen hasta hoy (incluida la de hoy) sin pagar + multas pendientes.
- **Para cancelar todo hoy** = el mismo `valorParaCancelar` del prestamista (respeta el interés por días si el contrato lo dice).
- **Recibos:** la misma plantilla del prestamista, sin el botón de WhatsApp.
- Todo es GET: el cliente nunca envía datos para guardar.

### El simulador

`SimularAbono` carga una **copia** del préstamo recién reconstruida desde la base de datos, aplica el abono con el mismo
`registrarPago` de un pago real y lee el capital y el interés de la próxima cuota antes y después. La copia se descarta:
no se guarda nada. Por eso es una consulta (GET) y un cliente de solo lectura puede usarla, y el resultado es exactamente
el que daría el pago real. Ejemplo (caso 2, capital vigente): abonar 660.000 el 10 ene → capital de 2.000.000 a 1.500.000
e interés de la próxima cuota de 160.000 a 120.000. Con capital inicial, el interés no cambia.

### Complejidad

| Operación | Complejidad | Por qué |
|---|---|---|
| Login del cliente | O(1) + BCrypt | búsqueda en la tabla hash; BCrypt es lento a propósito (constante) |
| Revisar una petición del prestamista | O(1) | solo mira la ruta |
| Revisar `/mis-prestamos/{id}` de un cliente | O(p × n) | reconstruye el préstamo para saber de quién es |
| Simular un abono | O(p × n) | reconstruye la copia y aplica un pago |

### Preguntas que podría hacer el profesor

**1. Si un cliente conoce la dirección para registrar pagos, ¿puede registrar uno?**

No. Toda petición pasa primero por `InterceptorRoles`, antes de llegar a cualquier controlador. Para un cliente, cualquier
método distinto de GET responde 403 (salvo cerrar su sesión), sin importar si el botón se ve o no en la página. Las pruebas
mandan el POST directamente (como lo haría alguien con `curl`) y comprueban que la respuesta es 403 y que el préstamo
sigue con los mismos pagos. Ocultar el botón en la vista es solo comodidad; la protección real está en el servidor.

**2. ¿Cómo evitan que un cliente vea el préstamo de otro cambiando el número en la dirección?**

Para las direcciones `/mis-prestamos/{id}`, el interceptor lee ese préstamo y compara su cédula con la cédula guardada en
la sesión; si no coinciden, responde 403. Si el préstamo no existe, también responde 403, para no dar pistas de qué números
existen. Además, al cliente no se le permite ninguna ruta del prestamista (`/prestamos/...`, `/clientes/...`), ni siquiera
la de su propio préstamo.

**3. ¿Dónde está guardada la contraseña del prestamista? ¿Y el PIN de los clientes?**

En ninguna parte en texto plano. La configuración tiene solo el hash BCrypt de la contraseña, y la base de datos solo el
hash de cada PIN. Al entrar, BCrypt calcula si lo que se escribió corresponde a ese hash; no hay forma de obtener la clave
original a partir del hash. Como BCrypt agrega una sal aleatoria, dos clientes con el mismo PIN tienen hashes distintos, y
como es lento a propósito, probar miles de PIN por fuerza bruta resulta costoso.

**4. ¿Por qué el usuario no escoge su rol al entrar?**

Porque el rol no es algo que el usuario declare: es un dato de su cuenta. Si el formulario dejara escoger "soy prestamista"
o "soy cliente", el rol dependería de lo que alguien marca, y sería una puerta más que proteger (y una fuente de errores:
un cliente que marca la opción equivocada no entra y no sabe por qué). Con un solo formulario, `IniciarSesion.entrar` deduce
el rol de la cuenta: si el identificador es el usuario del prestamista, verifica su contraseña; si no, lo busca como cédula
en la tabla hash y verifica el PIN. Para que no haya ambigüedad, el usuario del prestamista no puede ser numérico, así que
nunca coincide con una cédula. Y el mensaje de error es siempre el mismo, para no revelar si la cuenta existe.

---

## Preguntas generales del proyecto

**1. ¿Por qué no usaron JPA/Hibernate?**

Porque el enunciado pide JDBC con SQL escrito a mano, y además encaja con el diseño: la base de datos solo guarda lo que
pasó (contratos, pagos, condonaciones) y el dominio calcula todo lo demás con nuestras estructuras. Con JPA, el modelo del
dominio quedaría atado a las tablas (anotaciones, setters, colecciones de `java.util`), y eso rompería las reglas del
dominio. Con `JdbcTemplate`, cada consulta se lee tal cual en el código y el repositorio solo traduce filas a objetos.

**2. ¿Por qué H2 y cómo pasarían a PostgreSQL?**

H2 corre dentro de la aplicación y guarda en un archivo (`./data/cartera`): no hay que instalar nada para la demo. Está
en modo PostgreSQL y las migraciones usan SQL estándar (`GENERATED ALWAYS AS IDENTITY`, `DATE '...'`, `CHECK`). Para pasar a
PostgreSQL se agregan dos dependencias al `pom.xml` (el driver de PostgreSQL y el módulo de Flyway para PostgreSQL) y se
definen `DB_URL`, `DB_USUARIO` y `DB_CLAVE`. El SQL de las migraciones y el código Java no cambian.

**3. ¿Qué ganaron con la arquitectura hexagonal?**

El centro (dominio y casos de uso) no sabe que existen Spring, JDBC ni la web. Eso permite probarlo sin base de datos ni
servidor: las pruebas del dominio y de los casos de uso corren en milisegundos, con repositorios falsos hechos con nuestras
propias estructuras. Cambiar H2 por PostgreSQL, o JDBC por otra tecnología, solo toca un adaptador.

**4. ¿Por qué no usan `ArrayList`, `HashMap`, `PriorityQueue` ni `Stack`?**

Porque el objetivo del proyecto es implementar y usar estructuras propias. En `dominio` y `aplicacion` no hay ninguna
colección de `java.util` (lo verificamos con `grep`): solo `Iterator` y `NoSuchElementException` (necesarios para el
for-each) y `Comparator` (para que la cola de prioridad sepa ordenar). Ninguno de esos es una colección. Incluso los
repositorios JDBC agregan cada fila directamente a una `ListaDoble`.

**5. ¿Cómo saben que todo funciona?**

Hay 330 pruebas automáticas (`./mvnw test`): de cada estructura, del dominio (incluidos los 5 casos calculados a mano), de
las políticas, de los casos de uso (sin Spring), de la persistencia (contra H2 real con Flyway), de las pantallas (con
`MockMvc`) y del interceptor de roles (enviando peticiones prohibidas directamente). Además, los datos de prueba tienen su
propia prueba que comprueba el estado de cada préstamo a una fecha fija. Y la aplicación se probó en vivo: así encontramos
el problema de `;jsessionid`, que las pruebas automáticas no veían.

---

## Lista de verificación contra el enunciado

Cada requisito de `CLAUDE.md` y dónde se cumple.

**Stack y configuración**

| Requisito | Dónde |
|---|---|
| Java 25, Maven con `./mvnw`, Spring Boot, Spring MVC, Thymeleaf | `pom.xml` |
| JDBC con `JdbcTemplate`, SQL a mano, sin JPA | `RepositorioClientesJdbc`, `RepositorioPrestamosJdbc` |
| H2 en archivo `./data/cartera`, `MODE=PostgreSQL`, SQL estándar; `data/` en `.gitignore` | `application.properties`, `.gitignore`, `V1__crea_esquema.sql` |
| Migraciones Flyway | `db/migration/V1__crea_esquema.sql`, `V2__datos_de_prueba.sql` |
| Pruebas con JUnit 5 | `src/test` (330 pruebas) |
| Solo `spring-security-crypto` para BCrypt | `pom.xml`, `CifradorPinBCrypt` |
| Conexión con variables de entorno y valores por defecto | `application.properties` (`DB_URL`, `DB_USUARIO`, `DB_CLAVE`) |

**Arquitectura y reglas del código**

| Requisito | Dónde |
|---|---|
| Hexagonal: `dominio` / `aplicacion` (+ `puertos`) / `adaptadores` | paquetes bajo `io.github.juand13go.cartera` |
| `dominio` sin Spring, sin SQL, sin `aplicacion` ni `adaptadores` | verificado con `grep` (0 importaciones) |
| Sin colecciones de `java.util` en `dominio` y `aplicacion` | verificado con `grep` (solo `Iterator`, `Comparator`, `NoSuchElementException`) |
| Strategy para las políticas | `Frecuencia` (3 clases), `PoliticaMora` (4 clases) |
| "Hoy" con `Clock` en `America/Bogota`; fecha simulable con `app.fecha-simulada` | `ConfiguracionReloj` |
| Dinero `BigDecimal` con `HALF_UP` al peso; fechas `LocalDate` | `Prestamo.interesProximaCuota`, políticas de mora |
| Dominio sin setters, con acciones de negocio | verificado con `grep` (0 setters en `dominio`) |

**Estructuras de datos**

| Requisito | Dónde |
|---|---|
| `ListaDoble`, `TablaHash` (encadenamiento, redimensión a 0,75), `ColaPrioridad` (montículo sobre arreglo con `Comparator`), `Pila` | `dominio/estructuras` |
| Genéricas e `Iterable` | las cuatro clases |
| Pruebas propias de cada una | `ListaDobleTest`, `TablaHashTest`, `ColaPrioridadTest`, `PilaTest` |
| Javadoc con la complejidad de cada operación | las cuatro clases |

**Dominio y reglas del rédito**

| Requisito | Dónde |
|---|---|
| `Cuota` con el diseño del estudiante, estado calculado, `cubrir`, `saldoPendiente` | `Cuota`, `CuotaTest` |
| `Cliente`, `Prestamo`, `Contrato`, `Pago`, `Recibo` | `dominio/modelo` |
| Pagos al préstamo, no a una cuota; el préstamo los reparte | `Prestamo.registrarPago` |
| Cronograma que crece un periodo a la vez | `Prestamo.actualizarCronograma` |
| Plazo máximo de 24 meses con capital en la última cuota | `Prestamo.agregarSiguienteCuota`, `Frecuencia.cuotasEnPlazoMaximo` |
| Nada se capitaliza | multas y cuotas pendientes son deuda aparte (`saldoTotal`) |
| Las 11 variables del contrato con sus valores por defecto | `Contrato.Borrador`, formulario de préstamo |
| Cancelación anticipada sin intereses futuros | `Prestamo.valorParaCancelar`, `Prestamo.cancelar` |
| Cupo con confirmación explícita | `CrearPrestamo`, `CupoExcedidoException`, formulario de préstamo |
| Condonar la multa de una cuota | `Prestamo.condonarMulta`, botón "Condonar" |
| Deshacer = sacar de la pila y recalcular | `Prestamo.deshacerUltimoPago` |
| Casos de prueba 1 a 5 | `PrestamoTest` (`caso1...` a `caso4...`), `CuotaTest` (caso 5) |

**Interfaz web**

| Requisito | Dónde |
|---|---|
| Dos roles, mismo login; prestamista con contraseña BCrypt de la configuración; cliente con cédula + PIN | `login.html` (un solo formulario, `POST /login`), `IniciarSesion.entrar` |
| Sesión HTTP | `SesionWeb` |
| `InterceptorRoles` bloquea POST del cliente y préstamos ajenos en el backend | `InterceptorRoles`, `InterceptorRolesTest` |
| Panel: agenda (ColaPrioridad por fecha) y mora (ColaPrioridad por días de atraso) | `/`, `ArmarPanel` |
| Clientes: listar, crear, buscar por cédula (TablaHash) | `/clientes`, `DirectorioClientes` |
| Crear préstamo con todas las variables y valores por defecto | `/prestamos/nuevo` |
| Detalle: cronograma con estado, capital, pagos, pagar, deshacer, condonar, valor para cancelar hoy | `/prestamos/{id}` |
| Recibo imprimible con enlace `wa.me/57...` | `/prestamos/{id}/recibos/{n}` |
| Cliente: préstamos con barra de progreso, próxima fecha y valor para estar al día | `/mis-prestamos` |
| Cliente: historial de pagos con recibos | `/mis-prestamos/{id}` |
| Cliente: simulador "si abono $X hoy" | `/mis-prestamos/{id}?abono=...`, `SimularAbono` |
| Cliente: valor para cancelar todo hoy | `/mis-prestamos` y `/mis-prestamos/{id}` |
| Datos de prueba inventados (≥ 3 clientes, distintos estados, uno en mora) | `V2__datos_de_prueba.sql`, `DatosDePruebaTest` |

**Documentación**

| Requisito | Dónde |
|---|---|
| `README.md` (qué es, cómo correrlo, arquitectura, estructuras, usuarios de prueba) | `README.md` |
| Guía de sustentación por hito, con 3 preguntas por hito | este archivo |
