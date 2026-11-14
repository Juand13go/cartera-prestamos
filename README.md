# Cartera de préstamos (rédito)

Sistema web para **un prestamista** que maneja préstamos tipo **rédito**: cada periodo el cliente paga el interés y,
cuando puede, abona a capital; el préstamo se cierra cuando el capital llega a cero.

Proyecto de aula de **Estructura de Datos** (ITM). Lo que se evalúa: que el sistema use **estructuras de datos
implementadas a mano** (lista doblemente enlazada, tabla hash, cola de prioridad y pila) y que tenga una
**interfaz web** para usarlas.

## Cómo correrlo

Requisitos: **Java 25**. Maven no hace falta instalarlo: el proyecto trae `./mvnw`.

```bash
./mvnw spring-boot:run     # arranca la aplicación en http://localhost:8080
./mvnw test                # corre todas las pruebas
```

La primera vez, Flyway crea la base de datos H2 en `./data/cartera` y carga los datos de prueba.

| Para qué | Cómo |
|---|---|
| Simular otra fecha (ver cómo crece la mora) | `FECHA_SIMULADA=2027-01-20 ./mvnw spring-boot:run` |
| Ver las tablas en la consola de H2 | `H2_CONSOLA=true ./mvnw spring-boot:run` → http://localhost:8080/h2-console (URL `jdbc:h2:file:./data/cartera`, usuario `sa`, sin clave) |
| Volver a los datos de prueba originales | detener la aplicación y borrar la carpeta `data/` |
| Usar PostgreSQL en vez de H2 | agregar al `pom.xml` el driver `org.postgresql:postgresql` y `org.flywaydb:flyway-database-postgresql`, y definir `DB_URL`, `DB_USUARIO` y `DB_CLAVE`. El SQL y el código no cambian |
| Cambiar el acceso del prestamista | `PRESTAMISTA_USUARIO` y `PRESTAMISTA_CLAVE_CIFRADA` (hash BCrypt) |
| Cambiar el monto mínimo de un préstamo | `MONTO_MINIMO` (por defecto 50000) |

## Usuarios de prueba (datos inventados)

Todos entran por el mismo formulario ("Cédula o usuario" y "Contraseña"): el sistema reconoce si es el prestamista o un
cliente a partir de la cuenta; nadie escoge su rol.

| Rol | Entra con | Qué se ve |
|---|---|---|
| Prestamista | `prestamista` / `cartera2026` | Todo: panel, clientes, préstamos, pagos, recibos |
| Cliente Laura | cédula `1000000001`, PIN `1234` | Dos préstamos al día (uno mensual y uno semanal) |
| Cliente Carlos | cédula `1000000002`, PIN `1234` | Préstamo en mora, con multas |
| Cliente Marta | cédula `1000000003`, PIN `1234` | Quincenal, capital vigente, abonó a capital; una cuota que caía domingo pasa al lunes |
| Cliente Jorge | cédula `1000000004`, PIN `1234` | Préstamo cerrado por cancelación anticipada |

El cliente solo puede **ver** sus propios préstamos; cualquier otra cosa la bloquea el servidor (403).

## Qué hace

**Prestamista**
- Panel con tres tarjetas de resumen (cobros de hoy, en mora, cartera total), la **agenda de cobros**
  (próximos 7 días) y la **lista de mora** (del más atrasado al menos).
- Clientes: listar, crear (con PIN), **buscar por cédula**, ficha con notas internas y deuda frente al cupo.
- Crear préstamo con las 11 condiciones del contrato; si supera el cupo, pide confirmación.
- Detalle del préstamo: cronograma con estados y multas, pagos, **registrar pago**, **deshacer el último pago**,
  **condonar** la multa de una cuota y **cancelar todo** hoy.
- Recibo imprimible de cada pago, con enlace para enviarlo por **WhatsApp**.

**Cliente** (solo lectura)
- Sus préstamos, pensados para celular: tarjeta "Próximo pago", barra de progreso, valor para estar al día
  y valor para cancelar hoy.
- Historial de pagos con sus recibos.
- Simulador: "si abono $X hoy", cómo quedarían el capital y, con capital vigente, el interés.

## Estructuras de datos

Implementadas a mano en `dominio/estructuras`, genéricas e `Iterable` (se recorren con for-each y con `th:each`).
En `dominio` y `aplicacion` no se usa ninguna colección de `java.util`.

| Estructura | Dónde se usa | Operaciones principales |
|---|---|---|
| `ListaDoble<T>` | Cronograma de cuotas de cada préstamo (crece una cuota a la vez) | `agregarAlFinal`, `agregarAlInicio`, `primero`, `ultimo`, `eliminarPrimero`, `eliminarUltimo`: **O(1)**; `obtener(i)`: O(n) |
| `TablaHash<K,V>` | Clientes por cédula (buscador y login); condonaciones de multa de cada préstamo | `guardar`, `buscar`, `eliminar`: **O(1) promedio**, O(n) peor caso; encadenamiento y redimensión al pasar factor de carga 0,75 (amortizado O(1)) |
| `ColaPrioridad<T>` | Agenda de cobros (por fecha) y lista de mora (por días de atraso, comparador invertido) | `insertar`, `extraer`: **O(log n)**; `verPrimero`: O(1); montículo binario sobre un arreglo |
| `Pila<T>` | Historial de pagos de cada préstamo; deshacer el último | `apilar`, `desapilar`, `verTope`: **O(1)** |

Cada operación tiene en su Javadoc la complejidad y el porqué; cada estructura tiene sus pruebas.

## Arquitectura (hexagonal ligera)

```
io.github.juand13go.cartera
├── dominio/            Java puro: sin Spring, sin SQL, sin colecciones de java.util
│   ├── estructuras/    ListaDoble, TablaHash, ColaPrioridad, Pila
│   ├── modelo/         Cliente, Contrato (+ Borrador), Prestamo, Cuota, Pago, Recibo
│   └── politicas/      Frecuencia y PoliticaMora (patrón Strategy), Calendario, OrdenImputacion...
├── aplicacion/         casos de uso (CrearPrestamo, RegistrarPago, ArmarPanel, SimularAbono...)
│   └── puertos/        interfaces: RepositorioClientes, RepositorioPrestamos, CifradorPin
└── adaptadores/
    ├── persistencia/   repositorios JDBC (JdbcTemplate, SQL escrito a mano)
    ├── seguridad/      BCrypt
    ├── web/            controladores Spring MVC, InterceptorRoles
    └── configuracion/  reloj del sistema y conexión de los casos de uso
```

Ideas clave:
- **La base de datos guarda, las estructuras calculan.** Solo se guardan clientes, contratos, pagos y condonaciones.
  Cuotas, estados, multas y saldos se calculan: al leer un préstamo se reconstruye repitiendo sus pagos en orden.
- **Deshacer** = sacar el último pago de la pila y recalcular desde el contrato.
- El **"hoy"** viene de un `Clock` inyectado (zona `America/Bogota`); por eso se puede simular otra fecha.
- **Dinero** con `BigDecimal`, redondeado al peso (`HALF_UP`). Fechas con `LocalDate`.
- El dominio no tiene setters: expone acciones de negocio (`registrarPago`, `condonarMulta`, `cancelar`...).

## Reglas del rédito

- Cada periodo genera una cuota = base del interés × tasa del periodo. Lo que se pague de más va a capital.
- El cronograma crece una cuota a la vez: la siguiente nace el día después de que vence la anterior.
- Plazo máximo de 24 meses: la última cuota incluye todo el capital pendiente.
- Ningún valor pendiente se capitaliza: interés y multas sin pagar quedan como deuda aparte.
- Cancelar antes: lo vencido + capital + interés del periodo en curso según el contrato; nunca intereses futuros.

| Condición del contrato | Opciones | Por defecto |
|---|---|---|
| Base del interés | capital inicial / capital vigente | capital inicial |
| Frecuencia | mensual (día del mes) / quincenal (15 y último) / semanal (día de la semana) | mensual, día del desembolso |
| Calendario | todos los días / sin domingos (pasa al lunes) | todos los días |
| Política de mora | sin multa / multa fija diaria / porcentaje único / porcentaje diario | sin multa |
| Días de gracia | entero ≥ 0 | 2 |
| Orden de imputación | multas primero / cuotas primero | multas primero |
| Al vencer el plazo | tratar como mora / seguir cobrando | tratar como mora |
| Interés al cancelar antes | periodo completo / por días corridos | periodo completo |

## Identidad visual

**Libreta — cartera de préstamos.** La idea es la libreta de cobro del prestamista, pero digital: ordenada,
confiable y cálida, no un banco frío. En cada pantalla lo primero que se ve es la plata y lo atrasado.

- **Tipografías** (están en `static/fuentes`, con su licencia OFL, así que funcionan sin internet):
  *Fraunces* para la marca, los títulos y las cifras grandes, porque es una serif cálida que recuerda la
  letra de una libreta; *IBM Plex Sans* (400, 500, 600) para el resto, porque se lee bien en tamaños pequeños.
  Los montos usan cifras de ancho fijo (`tabular-nums`) y van alineados a la derecha, para que las columnas cuadren.
- **Paleta** (variables CSS en `:root`): tinta `#0F4C45` (verde petróleo, el color principal), papel `#F7F5F0`,
  superficie `#FFFFFF`, texto `#1C2321`, texto suave `#5C6663` y bordes `#E2DED5`.
- **Estados de cuota** (texto sobre fondo): pagada `#2D794C`/`#E7F3EC`, con tiempo `#3B5B7A`/`#E8EEF4`,
  pagada con retraso `#926411`/`#FBF1DC`, mora `#B42318`/`#FDECEA`. Todos tienen contraste WCAG AA (≥ 4.5:1).
  El verde y el ámbar se oscurecieron un poco frente a la propuesta inicial porque no llegaban a 4.5:1.
  Cada etiqueta lleva el nombre del estado: el color acompaña, pero no es lo único que lo indica.
- **Sobriedad**: sin degradados, sin sombras fuertes, sin íconos de adorno y sin frameworks CSS ni librerías JS.
  Cuando la fecha es simulada, la barra lo avisa sobre fondo ámbar. Todo se ve bien desde 360 px de ancho,
  y el recibo se imprime solo como comprobante, sin barra ni botones.

## Tecnologías

Java 25 · Spring Boot (Spring MVC + Thymeleaf) · JDBC con `JdbcTemplate` (sin JPA) · H2 en modo PostgreSQL ·
Flyway · `spring-security-crypto` (solo BCrypt) · JUnit 5 · CSS propio, sin frameworks de frontend.

La guía de estudio para la sustentación está en [`docs/SUSTENTACION.md`](docs/SUSTENTACION.md).
