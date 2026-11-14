package io.github.juand13go.cartera.aplicacion.puertos;

import java.time.LocalDateTime;

/** Lo que la base de datos sabe de un pago y el dominio no: su id (número de recibo) y cuándo se registró. */
public record RegistroDePago(long id, LocalDateTime registradoEn) {
}
