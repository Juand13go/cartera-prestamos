package io.github.juand13go.cartera.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.politicas.FrecuenciaMensual;

class PrestamoTest {

    private static final LocalDate DESEMBOLSO_CASO_1 = LocalDate.of(2026, 11, 10);

    /** Caso 1: $2.000.000 al 8 % mensual, desembolso el 10 de noviembre de 2026, cobro el día 10. */
    private static Prestamo prestamoCaso1(BaseInteres base) {
        Contrato contrato = new Contrato(new BigDecimal("2000000"), new BigDecimal("8"), base,
                DESEMBOLSO_CASO_1, new FrecuenciaMensual(10));
        return new Prestamo("1001", contrato);
    }

    private static BigDecimal pesos(String valor) {
        return new BigDecimal(valor);
    }

    // Caso 1

    @Test
    void caso1AlDesembolsarSoloExisteLaPrimeraCuota() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);

        assertEquals(1, prestamo.getCronograma().tamano());
        assertEquals(LocalDate.of(2026, 12, 10), prestamo.getCronograma().primero().getFechaVencimiento());
        assertEquals(pesos("2000000"), prestamo.getCapitalPendiente());
    }

    @Test
    void caso1LaSiguienteCuotaNaceElDiaDespuesDelVencimiento() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);

        prestamo.actualizarCronograma(LocalDate.of(2026, 12, 10));
        assertEquals(1, prestamo.getCronograma().tamano());

        prestamo.actualizarCronograma(LocalDate.of(2026, 12, 11));
        assertEquals(2, prestamo.getCronograma().tamano());
    }

    @Test
    void caso1CuatroCuotasDe160000LosDias10() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);

        prestamo.actualizarCronograma(LocalDate.of(2027, 2, 11));

        LocalDate[] fechasEsperadas = {
                LocalDate.of(2026, 12, 10), LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 2, 10), LocalDate.of(2027, 3, 10)};
        assertEquals(4, prestamo.getCronograma().tamano());
        int i = 0;
        for (Cuota cuota : prestamo.getCronograma()) {
            assertEquals(i + 1, cuota.getNumero());
            assertEquals(fechasEsperadas[i], cuota.getFechaVencimiento());
            assertEquals(pesos("160000"), cuota.getValor());
            i++;
        }
    }

    @Test
    void actualizarConUnaFechaAnteriorNoGeneraNiBorraCuotas() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);
        prestamo.actualizarCronograma(LocalDate.of(2027, 1, 20));

        prestamo.actualizarCronograma(LocalDate.of(2026, 12, 1));

        assertEquals(3, prestamo.getCronograma().tamano());
    }

    // Caso 4

    @Test
    void caso4CuotaDe400000YFebreroVenceElUltimoDia() {
        Contrato contrato = new Contrato(pesos("8000000"), pesos("5"), BaseInteres.CAPITAL_INICIAL,
                LocalDate.of(2026, 11, 30), new FrecuenciaMensual(30));
        Prestamo prestamo = new Prestamo("1002", contrato);

        prestamo.actualizarCronograma(LocalDate.of(2027, 3, 1));

        assertEquals(4, prestamo.getCronograma().tamano());
        assertEquals(pesos("400000"), prestamo.getCronograma().primero().getValor());
        assertEquals(LocalDate.of(2027, 2, 28), prestamo.getCronograma().obtener(2).getFechaVencimiento());
        assertEquals(LocalDate.of(2027, 3, 30), prestamo.getCronograma().ultimo().getFechaVencimiento());
    }

    // Redondeo

    @Test
    void elInteresSeRedondeaAlPesoHaciaArribaDesdeLaMitad() {
        // 150.050 × 1 % = 1.500,50 → 1.501
        Contrato contrato = new Contrato(pesos("150050"), pesos("1"), BaseInteres.CAPITAL_INICIAL,
                DESEMBOLSO_CASO_1, new FrecuenciaMensual(10));

        assertEquals(pesos("1501"), new Prestamo("1003", contrato).getCronograma().primero().getValor());
    }

    @Test
    void elInteresSeRedondeaAlPesoHaciaAbajoAntesDeLaMitad() {
        // 1.234.567 × 3 % = 37.037,01 → 37.037
        Contrato contrato = new Contrato(pesos("1234567"), pesos("3"), BaseInteres.CAPITAL_INICIAL,
                DESEMBOLSO_CASO_1, new FrecuenciaMensual(10));

        assertEquals(pesos("37037"), new Prestamo("1004", contrato).getCronograma().primero().getValor());
    }

    // Validaciones del contrato y del préstamo

    @Test
    void contratoConCapitalOTasaNoPositivosLanzaExcepcion() {
        FrecuenciaMensual mensual = new FrecuenciaMensual(10);

        assertThrows(IllegalArgumentException.class, () -> new Contrato(BigDecimal.ZERO, pesos("8"),
                BaseInteres.CAPITAL_INICIAL, DESEMBOLSO_CASO_1, mensual));
        assertThrows(IllegalArgumentException.class, () -> new Contrato(pesos("2000000"), pesos("-1"),
                BaseInteres.CAPITAL_INICIAL, DESEMBOLSO_CASO_1, mensual));
    }

    @Test
    void contratoSinBaseFechaOFrecuenciaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Contrato(pesos("2000000"), pesos("8"),
                null, DESEMBOLSO_CASO_1, new FrecuenciaMensual(10)));
        assertThrows(IllegalArgumentException.class, () -> new Contrato(pesos("2000000"), pesos("8"),
                BaseInteres.CAPITAL_INICIAL, null, new FrecuenciaMensual(10)));
        assertThrows(IllegalArgumentException.class, () -> new Contrato(pesos("2000000"), pesos("8"),
                BaseInteres.CAPITAL_INICIAL, DESEMBOLSO_CASO_1, null));
    }

    @Test
    void prestamoSinCedulaLanzaExcepcion() {
        Contrato contrato = new Contrato(pesos("2000000"), pesos("8"), BaseInteres.CAPITAL_INICIAL,
                DESEMBOLSO_CASO_1, new FrecuenciaMensual(10));

        assertThrows(IllegalArgumentException.class, () -> new Prestamo(" ", contrato));
    }
}
