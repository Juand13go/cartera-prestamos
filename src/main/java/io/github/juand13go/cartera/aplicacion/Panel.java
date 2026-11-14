package io.github.juand13go.cartera.aplicacion;

import java.time.LocalDate;

import io.github.juand13go.cartera.dominio.estructuras.ColaPrioridad;

/**
 * Lo que ve el prestamista al entrar: la agenda de cobros y la lista de mora, cada una en su cola de prioridad.
 * Al recorrerlas con for-each (o th:each) salen en orden de prioridad sin vaciarse.
 */
public record Panel(LocalDate hoy, ColaPrioridad<CobroAgendado> agenda, ColaPrioridad<PrestamoEnMora> mora) {
}
