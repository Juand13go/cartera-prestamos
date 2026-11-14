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

    // Días de atraso

    @Test
    void antesODelDiaDelVencimientoNoHayAtraso() {
        Cuota cuota = cuotaDeFebrero();

        assertEquals(0, cuota.diasDeAtraso(LocalDate.of(2027, 2, 1)));
        assertEquals(0, cuota.diasDeAtraso(VENCE_10_FEB));
    }

    @Test
    void sinCubrirElAtrasoCuentaHastaHoy() {
        Cuota cuota = cuotaDeFebrero();

        assertEquals(5, cuota.diasDeAtraso(LocalDate.of(2027, 2, 15)));
    }

    @Test
    void cubiertaElAtrasoSeCongelaElDiaEnQueSeCubrio() {
        Cuota cuota = cuotaDeFebrero();
        cuota.cubrir(new BigDecimal("160000"), LocalDate.of(2027, 2, 14));

        assertEquals(4, cuota.diasDeAtraso(LocalDate.of(2027, 3, 1)));
    }

    @Test
    void cubiertaATiempoNoTieneAtraso() {
        Cuota cuota = cuotaDeFebrero();
        cuota.cubrir(new BigDecimal("160000"), LocalDate.of(2027, 2, 5));

        assertEquals(0, cuota.diasDeAtraso(LocalDate.of(2027, 3, 1)));
    }

    // Condonación

    @Test
    void sinCondonarSeCobraTodaLaMultaCalculada() {
        Cuota cuota = cuotaDeFebrero();

        assertFalse(cuota.estaMultaCondonada());
        assertEquals(new BigDecimal("6000"), cuota.multaACobrar(new BigDecimal("6000")));
    }

    @Test
    void condonadaLaMultaQuedaFijaEnLoQueYaEstabaPagado() {
        Cuota cuota = cuotaDeFebrero();
        cuota.abonarMulta(new BigDecimal("4000"));

        cuota.condonarMulta(cuota.getMultaPagada());

        assertTrue(cuota.estaMultaCondonada());
        assertEquals(new BigDecimal("4000"), cuota.multaACobrar(new BigDecimal("6000")));
        assertEquals(new BigDecimal("4000"), cuota.multaACobrar(new BigDecimal("20000")));
    }

    @Test
    void noSePuedeCondonarDosVeces() {
        Cuota cuota = cuotaDeFebrero();
        cuota.condonarMulta(BigDecimal.ZERO);

        assertThrows(IllegalStateException.class, () -> cuota.condonarMulta(BigDecimal.ZERO));
    }

    // Cuota con capital (la última del plazo)

    @Test
    void unaCuotaNormalNoIncluyeCapital() {
        Cuota cuota = cuotaDeFebrero();

        assertEquals(BigDecimal.ZERO, cuota.getCapitalIncluido());
        assertEquals(new BigDecimal("160000"), cuota.getInteres());
        assertEquals(BigDecimal.ZERO, cuota.capitalSinPagar());
    }

    @Test
    void laCuotaConCapitalValeInteresMasCapital() {
        Cuota ultima = new Cuota(24, LocalDate.of(2028, 11, 10), new BigDecimal("160000"), new BigDecimal("2000000"));

        assertEquals(new BigDecimal("2160000"), ultima.getValor());
        assertEquals(new BigDecimal("160000"), ultima.getInteres());
        assertEquals(new BigDecimal("2000000"), ultima.getCapitalIncluido());
    }

    @Test
    void dentroDeLaCuotaSePagaPrimeroElInteres() {
        Cuota ultima = new Cuota(24, LocalDate.of(2028, 11, 10), new BigDecimal("160000"), new BigDecimal("2000000"));

        ultima.cubrir(new BigDecimal("100000"), LocalDate.of(2028, 11, 1));
        assertEquals(new BigDecimal("2000000"), ultima.capitalSinPagar());

        ultima.cubrir(new BigDecimal("1060000"), LocalDate.of(2028, 11, 2));
        assertEquals(new BigDecimal("1000000"), ultima.capitalSinPagar());
    }

    // Rebaja de interés al cancelar por días

    @Test
    void sinPagosSeRebajaTodoLoQueExcedeElInteresCausado() {
        Cuota cuota = cuotaDeFebrero();

        assertEquals(new BigDecimal("80000"), cuota.rebajaDeInteres(new BigDecimal("80000")));
    }

    @Test
    void loYaPagadoDeInteresNoSeDevuelve() {
        Cuota cuota = cuotaDeFebrero();
        cuota.cubrir(new BigDecimal("100000"), LocalDate.of(2027, 1, 20));

        // Causado 80.000 pero ya pagó 100.000: solo se rebajan los 60.000 que faltaban.
        assertEquals(new BigDecimal("60000"), cuota.rebajaDeInteres(new BigDecimal("80000")));
    }

    @Test
    void rebajarElInteresPuedeDejarLaCuotaCubierta() {
        Cuota cuota = cuotaDeFebrero();
        cuota.cubrir(new BigDecimal("100000"), LocalDate.of(2027, 1, 20));

        cuota.rebajarInteres(new BigDecimal("60000"), LocalDate.of(2027, 1, 25));

        assertEquals(new BigDecimal("100000"), cuota.getValor());
        assertTrue(cuota.estaCubierta());
        assertEquals(LocalDate.of(2027, 1, 25), cuota.getFechaCubierta());
    }
}
