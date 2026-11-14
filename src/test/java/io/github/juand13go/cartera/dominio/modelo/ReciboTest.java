package io.github.juand13go.cartera.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ReciboTest {

    @Test
    void elReciboJuntaElPagoConSuNumeroYHora() {
        Cliente cliente = new Cliente("1000000001", "Laura Gómez", "3000000001", null, null,
                new BigDecimal("5000000"), null, "cifrado");
        Contrato contrato = Contrato.borrador(new BigDecimal("2000000"), new BigDecimal("8"),
                LocalDate.of(2026, 11, 10)).firmar();
        Prestamo prestamo = new Prestamo(7, cliente.getCedula(), contrato);
        Pago pago = prestamo.registrarPago(LocalDate.of(2026, 12, 10), new BigDecimal("160000"));
        LocalDateTime registrado = LocalDateTime.of(2026, 12, 10, 9, 30);

        Recibo recibo = new Recibo(42, registrado, prestamo.getId(), cliente, pago);

        assertEquals(42, recibo.getNumero());
        assertEquals(registrado, recibo.getFechaHora());
        assertEquals(7, recibo.getPrestamoId());
        assertSame(cliente, recibo.getCliente());
        assertSame(pago, recibo.getPago());
        assertEquals(new BigDecimal("160000"), recibo.getValor());
        assertEquals(1, recibo.getCuotasCubiertas().tamano());
        assertEquals(1, recibo.getCuotasCubiertas().primero());
        assertEquals(0, new BigDecimal("2000000").compareTo(recibo.getSaldoRestante()));
    }
}
