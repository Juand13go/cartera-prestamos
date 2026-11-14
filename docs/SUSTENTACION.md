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

---

## Hito 2: Dominio del rédito

### Qué se construyó

El corazón del negocio en Java puro (sin Spring, sin base de datos): un préstamo tipo **rédito** que
genera sus cuotas de interés a medida que pasa el tiempo, recibe pagos, los reparte y se cierra cuando
el capital llega a 0.

| Clase | Paquete | Qué es |
|---|---|---|
| `Cuota` | `dominio.modelo` | Lo que se debe pagar en una fecha. Sabe cuánto le han cubierto y **calcula** su estado |
| `EstadoCuota` | `dominio.modelo` | `CON_TIEMPO`, `PAGADA`, `PAGADA_CON_RETRASO`, `MORA` |
| `BaseInteres` | `dominio.modelo` | `CAPITAL_INICIAL` (el interés nunca baja) o `CAPITAL_VIGENTE` (baja si se abona a capital) |
| `Contrato` | `dominio.modelo` | Las condiciones: capital, tasa, base, fecha de desembolso, frecuencia. Todo `final`, sin setters |
| `Prestamo` | `dominio.modelo` | Contrato + cronograma (`ListaDoble<Cuota>`) + pagos (`Pila<Pago>`) + capital pendiente |
| `Pago` | `dominio.modelo` | Fecha, monto y cómo se repartió: `abonoACuotas` + `abonoACapital` |
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
podrá simular otra fecha. El `Clock` de `America/Bogota` vivirá en la capa de aplicación.

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
| `saldoTotal()` | O(n) | suma el saldo de todas las cuotas |
| `registrarPago` | O(n + k) | actualiza el cronograma, calcula el saldo y recorre las cuotas una vez |
| `estaCerrado()` | O(1) | compara el capital con 0 |
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
