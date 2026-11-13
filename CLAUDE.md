# CLAUDE.md — cartera-prestamos

## Contexto

Proyecto de aula de **Estructura de Datos** (ITM). Sistema web para **un solo prestamista** informal
que maneja préstamos tipo **rédito**. Lo sustenta un estudiante el **miércoles 18 de noviembre de 2026**:
el código debe ser simple, legible y fácil de explicar en una sustentación oral. Nada de sobreingeniería.
Nombres de clases, métodos, variables, comentarios y mensajes de commit **en español**.

Lo que se evalúa: que el sistema use **estructuras de datos implementadas a mano** y que tenga
una **interfaz web** para interactuar con él.

## Stack (el proyecto ya existe, creado con Spring Initializr)

- Java 25, Maven (usar siempre `./mvnw`), Spring Boot con Spring MVC y Thymeleaf.
- Acceso a datos: **JDBC con `JdbcTemplate`, SQL escrito a mano. NO usar JPA/Hibernate.**
- Base de datos: **H2 en archivo** (`./data/cartera`) con `MODE=PostgreSQL` y SQL estándar,
  para poder migrar a PostgreSQL después cambiando solo la configuración. Agregar `data/` al `.gitignore`.
- Esquema: migraciones **Flyway** en `src/main/resources/db/migration` (`V1__...sql`, `V2__...sql`).
- Pruebas: JUnit 5.
- Contraseñas y PIN: solo la dependencia `spring-security-crypto` (BCrypt). **NO Spring Security completo.**
- Configuración de la conexión en `application.properties` con variables de entorno y valores por defecto.
- Paquete raíz: `io.github.juand13go.cartera`.
- Entorno: Ubuntu en WSL2. El proyecto está en `~/dev/cartera-prestamos`.

## Arquitectura: hexagonal ligera

```
io.github.juand13go.cartera
├── CarteraPrestamosApplication.java
├── dominio/            ← Java puro: SIN Spring, SIN SQL, SIN java.util para colecciones
│   ├── modelo/         Cliente, Prestamo, Contrato, Cuota, Pago, Recibo, enums
│   ├── politicas/      PoliticaMora (interfaz) y sus opciones, OrdenImputacion, Frecuencia...
│   └── estructuras/    ListaDoble, TablaHash, ColaPrioridad, Pila
├── aplicacion/         casos de uso (CrearPrestamo, RegistrarPago, ...)
│   └── puertos/        interfaces de repositorio
└── adaptadores/
    ├── web/            controladores Spring MVC, InterceptorRoles
    └── persistencia/   repositorios JDBC que implementan los puertos
```

Reglas:
- `dominio` no importa nada de `aplicacion` ni de `adaptadores`, ni nada de Spring.
- Patrón **Strategy** para las políticas del contrato (una interfaz, una clase por opción).
- El "hoy" del sistema se inyecta con `java.time.Clock` (zona `America/Bogota`), nunca `LocalDate.now()`
  suelto. En la demo debe poder simularse otra fecha (por ejemplo con una propiedad de configuración
  `app.fecha-simulada` opcional) para mostrar la mora en vivo.
- Dinero siempre `BigDecimal`, redondeo al peso con `RoundingMode.HALF_UP`. Fechas siempre `LocalDate`.
- Las clases del dominio **no exponen setters**: exponen acciones con sentido de negocio.
- La base de datos guarda; las estructuras calculan.

## Estructuras de datos (OBLIGATORIAS, implementación propia y genérica)

En `dominio` y `aplicacion` **NO** usar `ArrayList`, `LinkedList`, `HashMap`, `PriorityQueue`, `Stack`
ni otras colecciones de `java.util`. Todas las estructuras implementan `Iterable<T>` para poder
recorrerlas con for-each y con `th:each` en Thymeleaf.

| Estructura | Uso | Notas |
|---|---|---|
| `ListaDoble<T>` | Cronograma de cuotas de cada préstamo | En rédito crece un periodo a la vez |
| `TablaHash<K,V>` | Clientes por cédula (búsqueda y login del cliente) | Encadenamiento, redimensiona al pasar factor de carga 0.75 |
| `ColaPrioridad<T>` | Agenda de cobros del día y lista de mora | Montículo binario sobre arreglo, con `Comparator` |
| `Pila<T>` | Historial de pagos para deshacer el último | |

Cada estructura lleva:
- pruebas unitarias propias;
- un comentario Javadoc corto en cada operación con su complejidad (O grande) y por qué.

## La clase `Cuota` (ya la diseñó y escribió el estudiante; respetar el diseño)

Existe `dominio/modelo/Cuota.java` y `EstadoCuota.java`. Ajustarlos a este diseño sin cambiar la idea:

- Atributos: `numero` (int), `fechaVencimiento` (LocalDate), `valor` (BigDecimal),
  `valorCubierto` (BigDecimal, empieza en `BigDecimal.ZERO`), `fechaCubierta` (LocalDate, empieza en `null`).
- Constructor recibe solo `numero`, `fechaVencimiento` y `valor`. **Sin setters.**
- `cubrir(BigDecimal monto, LocalDate fecha)`: suma a `valorCubierto`; si la cuota queda completa,
  guarda `fecha` en `fechaCubierta`. No debe aceptar más de lo que falta (el préstamo es quien reparte).
- `estado(LocalDate hoy)` **se calcula, no se guarda**:
  - si está cubierta: `PAGADA` si `fechaCubierta` ≤ `fechaVencimiento`, si no `PAGADA_CON_RETRASO`;
  - si no está cubierta: `CON_TIEMPO` si `hoy` ≤ `fechaVencimiento`, si no `MORA`.
  - (los días de gracia afectan la multa, no el estado.)
- Agregar `saldoPendiente()`.

## Modelo del dominio

- `Cliente`: cédula (llave natural), nombre, teléfono, dirección, referencia, cupo máximo,
  notas internas (solo las ve el prestamista), PIN (hash BCrypt).
- `Prestamo`: id, cliente, `Contrato`, `ListaDoble<Cuota>` (lo que DEBE), pagos (lo que ENTREGÓ),
  capital pendiente. **Los pagos son al préstamo, no a una cuota**: el préstamo los reparte.
- `Contrato`: todas las condiciones, fijas desde que se crea el préstamo (ver variables abajo).
- `Pago`: id, fecha, monto, y el desglose de cómo se repartió (multas, cuotas, capital).
- `Recibo`: se genera por cada pago (número, fecha y hora, valor, desglose, cuotas cubiertas, saldo restante).
- Multas: se calculan según la política del contrato; cada cuota puede tener su multa condonada.

Un préstamo es su contrato más sus pagos en orden: aplicar los pagos en orden siempre produce el
mismo estado. **Deshacer** = sacar el último pago de la pila y recalcular desde el contrato.

## Reglas de negocio — SOLO plantilla RÉDITO

- Cada periodo genera una cuota = base del interés × tasa del periodo.
  Lo que el cliente pague por encima de lo exigible va a **capital**.
- El préstamo se cierra cuando el capital pendiente llega a 0.
- La tasa es un porcentaje nominal por periodo (ej. 8 % mensual), sin tasa efectiva anual.
- El cronograma NO se genera completo al inicio: crece un periodo a la vez en cada fecha de corte.
- Plazo máximo 24 meses: la cuota del último mes incluye el capital pendiente completo.
- Ningún valor pendiente se capitaliza: interés o multa sin pagar quedan como deuda aparte.

### Variables del contrato (se eligen en el formulario al crear el préstamo; no cambian después)

| Variable | Opciones | Por defecto |
|---|---|---|
| Capital | ≥ monto mínimo (configurable, 50.000) | — |
| Tasa por periodo (%) | libre | — |
| Base del interés | `CAPITAL_INICIAL` (el interés nunca baja), `CAPITAL_VIGENTE` (se recalcula en cada corte) | `CAPITAL_INICIAL` |
| Frecuencia | `MENSUAL`, `QUINCENAL` (día 15 y último día del mes), `SEMANAL` | `MENSUAL` |
| Día de cobro | mensual: día del mes; semanal: día de la semana. Si el mes no tiene ese día → último día del mes | el del desembolso |
| Calendario | todos los días cuentan / sin domingos (si cae domingo, pasa al lunes) | todos los días |
| Política de mora | `SinMulta`, `MultaFijaDiaria` (valor), `PorcentajeUnico` (% sobre la cuota, una vez), `PorcentajeDiario` (% sobre la cuota por día) | `SinMulta` |
| Días de gracia | entero ≥ 0; la multa cuenta desde que terminan | 2 |
| Orden de imputación | `MULTAS_PRIMERO` (multas → cuotas vencidas → cuota actual → capital), `CUOTAS_PRIMERO` (cuotas vencidas → cuota actual → multas → capital) | `MULTAS_PRIMERO` |
| Al vencer el plazo sin pagar | `TRATAR_COMO_MORA`, `SEGUIR_COBRANDO` (sigue generando interés mes a mes) | `TRATAR_COMO_MORA` |
| Interés al cancelar antes | `PERIODO_COMPLETO`, `POR_DIAS` (interés del periodo × días corridos / días del periodo) | `PERIODO_COMPLETO` |

- Cuotas vencidas se cubren de la más vieja a la más nueva.
- **Cancelación anticipada**: lo vencido + capital pendiente + interés del periodo en curso según contrato.
  Nunca se cobran intereses de periodos futuros.
- **Cupo**: un cliente puede tener varios préstamos activos. Si su deuda vigente + el préstamo nuevo
  supera su cupo, el sistema pide confirmación explícita antes de crear el préstamo.
- **Condonar mora**: el prestamista puede anular la multa de una cuota puntual.

### Casos de prueba obligatorios (calculados a mano por el estudiante)

1. $2.000.000 al 8 % mensual, desembolso 10 nov 2026, base capital inicial:
   cuotas el 10 dic, 10 ene, 10 feb, 10 mar, cada una de $160.000.
2. Mismo préstamo; el 10 ene paga $660.000 (160.000 de interés + 500.000 a capital):
   - base `CAPITAL_INICIAL`: las cuotas siguientes siguen en $160.000; capital pendiente $1.500.000;
   - base `CAPITAL_VIGENTE`: las cuotas siguientes bajan a $120.000.
3. El 10 feb solo paga $100.000: la cuota de febrero queda con $60.000 pendientes y en `MORA` después del vencimiento.
4. $8.000.000 al 5 % mensual, cobro el día 30 → cuota de $400.000; la de febrero vence el último día de febrero.
5. Estados de `Cuota` (cuota de $160.000 que vence el 10 feb):
   hoy 1 feb sin pagos → `CON_TIEMPO`; cubierta el 5 feb → `PAGADA`; cubierta el 14 feb → `PAGADA_CON_RETRASO`;
   hoy 15 feb con $100.000 → `MORA`.

## Interfaz web (mínima, pero completa; Thymeleaf + CSS simple, sin frameworks de frontend)

Dos roles, mismo login de la aplicación:
- **PRESTAMISTA** (usuario y contraseña definidos por configuración, contraseña con BCrypt): puede todo.
- **CLIENTE** (entra con cédula + PIN): **solo lectura y solo sus propios préstamos**.

Un `HandlerInterceptor` (`InterceptorRoles`) bloquea en el backend cualquier petición que modifique
datos (POST) si el rol es CLIENTE, y cualquier intento de ver préstamos de otro cliente. Ocultar
botones en la vista no es suficiente. Sesión HTTP para el login.

Pantallas del prestamista:
1. Panel: **agenda de cobros del día** (ColaPrioridad por fecha) y **lista de mora** (ColaPrioridad por días de atraso).
2. Clientes: listar, crear, buscar por cédula (TablaHash).
3. Crear préstamo: formulario con todas las variables del contrato y sus valores por defecto precargados.
4. Detalle del préstamo: cronograma de cuotas con estado, capital pendiente, historial de pagos,
   botones: registrar pago, deshacer último pago, condonar mora de una cuota, valor para cancelar hoy.
5. Recibo de cada pago (imprimible) y enlace `https://wa.me/57<telefono>?text=...` con el texto del recibo.

Pantallas del cliente:
1. Sus préstamos con barra de progreso, próxima fecha de cobro y valor para estar al día.
2. Historial de pagos con recibos.
3. Simulador: "si abono $X hoy" → nuevo capital y, si la base es capital vigente, el nuevo interés.
4. Valor para cancelar todo hoy.

Datos de prueba inventados (nunca datos reales) cargados por una migración Flyway, con al menos
3 clientes y préstamos en distintos estados, incluido uno en mora.

## Forma de trabajar

- Trabajar por **hitos**, en este orden. Empezar cada hito proponiendo un plan corto (qué archivos
  se crean o modifican y en qué orden) y esperar la aprobación del estudiante antes de escribir código.
- **Git lo maneja el estudiante. Claude NUNCA ejecuta comandos de git que modifiquen el repositorio**
  (`git add`, `git commit`, `git push`, `git reset`, `git checkout`, `git merge`, `git rebase`, etc.).
  Solo puede usar comandos de lectura como `git status`, `git diff` o `git log`.
- Al terminar cada funcionalidad o modificación: correr `./mvnw test` hasta que esté en verde, **detenerse**
  y entregar al estudiante:
  1. un resumen corto de lo que cambió;
  2. el comando `git add` exacto con los archivos de ese commit;
  3. el mensaje de commit listo para copiar.
  Luego esperar a que el estudiante diga que ya hizo el commit antes de continuar.
- Mensajes de commit con **Conventional Commits en español**:
  - formato `tipo(ámbito): descripción`, con tipos `feat`, `fix`, `test`, `refactor`, `docs`, `chore`;
    ámbito opcional y corto (`estructuras`, `dominio`, `web`, `persistencia`...);
  - descripción en imperativo y en minúscula (`agrega`, `corrige`, `calcula`), sin punto final,
    máximo 72 caracteres en la primera línea;
  - si el cambio necesita contexto, una línea en blanco y un cuerpo breve que explique **por qué**,
    no qué (el qué ya lo muestra el diff);
  - un commit por funcionalidad o cambio lógico: no mezclar una estructura nueva con un arreglo de otra cosa.
  - Ejemplo: `feat(estructuras): agrega lista doblemente enlazada genérica`
- Después de cada hito, agregar una sección a **`docs/SUSTENTACION.md`** con:
  qué se construyó, qué clases intervienen, por qué se eligió cada estructura, la complejidad de
  sus operaciones, y 3 preguntas que el profesor podría hacer sobre esa parte con su respuesta.
  Este archivo es para que el estudiante estudie; escríbelo claro y sin relleno.
- Código simple antes que ingenioso. Si hay que elegir entre una solución elegante y una fácil de
  explicar, elegir la fácil de explicar.

### Hitos

Dentro de cada hito puede haber varios commits (por ejemplo, el hito 1 tiene al menos cuatro:
uno por estructura con sus pruebas).

1. **Estructuras de datos**: `ListaDoble`, `TablaHash`, `ColaPrioridad`, `Pila`, con pruebas.
2. **Dominio del rédito**: ajustar `Cuota`; `Contrato`, `Prestamo`, `Pago`, generación de cuotas,
   `registrarPago` con imputación, capital vigente / inicial, cierre del préstamo. Casos de prueba 1 a 5.
3. **Políticas**: mora (4 opciones) con días de gracia, condonación, imputación (2 órdenes),
   cancelación anticipada, plazo máximo, deshacer con la pila.
4. **Persistencia**: migraciones Flyway, puertos y repositorios JDBC, datos de prueba.
5. **Web del prestamista**: panel con agenda y mora, clientes, crear préstamo, detalle, pagos, recibo.
6. **Login, roles y portal del cliente**: interceptor, vistas de solo lectura, simulador.
7. **Cierre**: `README.md` (qué es, cómo correrlo, arquitectura, estructuras y su complejidad,
   usuarios de prueba) y repaso final de `docs/SUSTENTACION.md`.

Si el tiempo no alcanza, se recorta desde el final de la lista de pantallas (simulador, WhatsApp),
nunca las estructuras de datos.