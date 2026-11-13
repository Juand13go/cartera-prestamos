package io.github.juand13go.cartera.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CuotaTest {

    private static final LocalDate VENCE_10_FEB = LocalDate.of(2027, 2, 10);

    /** Cuota del caso 5: $160.000 que vence el 10 de febrero. */
    private static Cuota cuotaDeFebrero() {
        return new Cuota(3, VENCE_10_FEB, new BigDecimal("160000"));
    }

    @Test
    void unaCuotaNuevaNoTieneNadaCubierto() {
        Cuota cuota = cuotaDeFebrero();

        assertEquals(BigDecimal.ZERO, cuota.getValorCubierto());
        assertEquals(new BigDecimal("160000"), cuota.saldoPendiente());
        assertNull(cuota.getFechaCubierta());
        assertFalse(cuota.estaCubierta());
    }

    // Caso 5: estados de la cuota

    @Test
    void sinPagosAntesDelVencimientoEstaConTiempo() {
        Cuota cuota = cuotaDeFebrero();

        assertEquals(EstadoCuota.CON_TIEMPO, cuota.estado(LocalDate.of(2027, 2, 1)));
    }

    @Test
    void elMismoDiaDelVencimientoSigueConTiempo() {
        Cuota cuota = cuotaDeFebrero();

        assertEquals(EstadoCuota.CON_TIEMPO, cuota.estado(VENCE_10_FEB));
    }

    @Test
    void cubiertaAntesDelVencimientoEstaPagada() {
        Cuota cuota = cuotaDeFebrero();
        cuota.cubrir(new BigDecimal("160000"), LocalDate.of(2027, 2, 5));

        assertEquals(EstadoCuota.PAGADA, cuota.estado(LocalDate.of(2027, 2, 5)));
        // Consultarla días después no cambia que se pagó a tiempo.
        assertEquals(EstadoCuota.PAGADA, cuota.estado(LocalDate.of(2027, 2, 15)));
    }

    @Test
    void cubiertaDespuesDelVencimientoEstaPagadaConRetraso() {
        Cuota cuota = cuotaDeFebrero();
        cuota.cubrir(new BigDecimal("160000"), LocalDate.of(2027, 2, 14));

        assertEquals(EstadoCuota.PAGADA_CON_RETRASO, cuota.estado(LocalDate.of(2027, 2, 14)));
    }

    @Test
    void conPagoParcialDespuesDelVencimientoEstaEnMora() {
        Cuota cuota = cuotaDeFebrero();
        cuota.cubrir(new BigDecimal("100000"), LocalDate.of(2027, 2, 8));

        assertEquals(EstadoCuota.MORA, cuota.estado(LocalDate.of(2027, 2, 15)));
        assertEquals(new BigDecimal("60000"), cuota.saldoPendiente());
    }

    // cubrir

    @Test
    void cubrirEnDosPartesGuardaLaFechaDelPagoQueLaCompleta() {
        Cuota cuota = cuotaDeFebrero();
        cuota.cubrir(new BigDecimal("100000"), LocalDate.of(2027, 2, 8));
        assertNull(cuota.getFechaCubierta());

        cuota.cubrir(new BigDecimal("60000"), LocalDate.of(2027, 2, 12));

        assertTrue(cuota.estaCubierta());
        assertEquals(LocalDate.of(2027, 2, 12), cuota.getFechaCubierta());
        assertEquals(EstadoCuota.PAGADA_CON_RETRASO, cuota.estado(LocalDate.of(2027, 2, 12)));
    }

    @Test
    void noAceptaMasDeLoQueFalta() {
        Cuota cuota = cuotaDeFebrero();
        cuota.cubrir(new BigDecimal("100000"), LocalDate.of(2027, 2, 8));

        assertThrows(IllegalArgumentException.class,
                () -> cuota.cubrir(new BigDecimal("60001"), LocalDate.of(2027, 2, 9)));
        assertEquals(new BigDecimal("60000"), cuota.saldoPendiente());
    }

    @Test
    void noAceptaMontosCeroONegativos() {
        Cuota cuota = cuotaDeFebrero();

        assertThrows(IllegalArgumentException.class,
                () -> cuota.cubrir(BigDecimal.ZERO, LocalDate.of(2027, 2, 8)));
        assertThrows(IllegalArgumentException.class,
                () -> cuota.cubrir(new BigDecimal("-5000"), LocalDate.of(2027, 2, 8)));
    }
}
