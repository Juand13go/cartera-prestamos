package io.github.juand13go.cartera.aplicacion;

import java.math.BigDecimal;

/**
 * Lo que llega del formulario para crear un cliente. Un record es una clase de solo datos:
 * Java le genera el constructor y un método por cada campo (por ejemplo, cedula()).
 * El PIN llega sin cifrar; CrearCliente lo cifra antes de guardarlo.
 */
public record DatosNuevoCliente(
        String cedula,
        String nombre,
        String telefono,
        String direccion,
        String referencia,
        BigDecimal cupoMaximo,
        String notas,
        String pin) {
}
