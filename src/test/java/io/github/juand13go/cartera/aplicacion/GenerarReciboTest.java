package io.github.juand13go.cartera.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.modelo.Cliente;
import io.github.juand13go.cartera.dominio.modelo.Contrato;
import io.github.juand13go.cartera.dominio.modelo.Recibo;

class GenerarReciboTest {

    private RepositorioPrestamosEnMemoria prestamos;
    private GenerarRecibo generarRecibo;
    private long id;

    @BeforeEach
    void preparar() {
        RepositorioClientesEnMemoria clientes = new RepositorioClientesEnMemoria();
        clientes.guardar(new Cliente("1000000001", "Laura Gómez", "3000000001", null, null,
                new BigDecimal("5000000"), null, "cifrado:1234"));
        prestamos = new RepositorioPrestamosEnMemoria();
        generarRecibo = new GenerarRecibo(prestamos, new DirectorioClientes(clientes));

        // Caso 2: paga 160.000 el 10 dic y 660.000 el 10 ene.
        id = prestamos.crear("1000000001", Contrato.borrador(new BigDecimal("2000000"), new BigDecimal("8"),
                LocalDate.of(2026, 11, 10)).firmar());
        new RegistrarPago(prestamos, ConsultarPrestamosTest.relojEn(LocalDate.of(2026, 12, 10)))
                .ejecutar(id, new BigDecimal("160000"));
        new RegistrarPago(prestamos, ConsultarPrestamosTest.relojEn(LocalDate.of(2027, 1, 10)))
                .ejecutar(id, new BigDecimal("660000"));
    }

    @Test
    void elReciboDelPrimerPagoTraeSuNumeroHoraYLoQueCubrio() {
        Recibo recibo = generarRecibo.ejecutar(id, 1);

        assertEquals(100, recibo.getNumero());
        assertEquals(LocalDateTime.of(2026, 12, 10, 12, 0), recibo.getFechaHora());
        assertEquals("Laura Gómez", recibo.getCliente().getNombre());
        assertEquals(0, new BigDecimal("160000").compareTo(recibo.getValor()));
        assertEquals(1, recibo.getCuotasCubiertas().primero());
        assertEquals(0, new BigDecimal("2000000").compareTo(recibo.getSaldoRestante()));
    }

    @Test
    void elReciboDelSegundoPagoMuestraElAbonoACapital() {
        Recibo recibo = generarRecibo.ejecutar(id, 2);

        assertEquals(101, recibo.getNumero());
        assertEquals(0, new BigDecimal("500000").compareTo(recibo.getPago().getAbonoACapital()));
        assertEquals(0, new BigDecimal("1500000").compareTo(recibo.getSaldoRestante()));
    }

    @Test
    void ultimoEsElReciboDelPagoMasReciente() {
        assertEquals(101, generarRecibo.ultimo(id).getNumero());
    }

    @Test
    void unPagoQueNoExisteLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> generarRecibo.ejecutar(id, 0));
        assertThrows(IllegalArgumentException.class, () -> generarRecibo.ejecutar(id, 3));
        assertThrows(IllegalArgumentException.class, () -> generarRecibo.ejecutar(999, 1));
    }

    @Test
    void despuesDeDeshacerYaNoExisteElReciboDelPagoAnulado() {
        new DeshacerUltimoPago(prestamos).ejecutar(id);

        assertThrows(IllegalArgumentException.class, () -> generarRecibo.ejecutar(id, 2));
        assertEquals(100, generarRecibo.ultimo(id).getNumero());
    }
}
