package io.github.juand13go.cartera.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.politicas.Calendario;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaMensual;

class PrestamoTest {

    private static final LocalDate DESEMBOLSO_CASO_1 = LocalDate.of(2026, 11, 10);

    /** Caso 1: $2.000.000 al 8 % mensual, desembolso el 10 de noviembre de 2026, cobro el día 10. */
    private static Prestamo prestamoCaso1(BaseInteres base) {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO_CASO_1)
                .base(base)
                .firmar();
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
        Contrato contrato = Contrato.borrador(pesos("8000000"), pesos("5"), LocalDate.of(2026, 11, 30))
                .frecuencia(new FrecuenciaMensual(30))
                .firmar();
        Prestamo prestamo = new Prestamo("1002", contrato);

        prestamo.actualizarCronograma(LocalDate.of(2027, 3, 1));

        assertEquals(4, prestamo.getCronograma().tamano());
        assertEquals(pesos("400000"), prestamo.getCronograma().primero().getValor());
        assertEquals(LocalDate.of(2027, 2, 28), prestamo.getCronograma().obtener(2).getFechaVencimiento());
        assertEquals(LocalDate.of(2027, 3, 30), prestamo.getCronograma().ultimo().getFechaVencimiento());
    }

    // Calendario

    @Test
    void sinDomingosLaCuotaQueCaeDomingoPasaAlLunesYLaSiguienteVuelveAlDia10() {
        // El 10 de enero de 2027 es domingo.
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO_CASO_1)
                .calendario(Calendario.SIN_DOMINGOS)
                .firmar();
        Prestamo prestamo = new Prestamo("1005", contrato);

        prestamo.actualizarCronograma(LocalDate.of(2027, 1, 12));

        assertEquals(LocalDate.of(2027, 1, 11), prestamo.getCronograma().obtener(1).getFechaVencimiento());
        assertEquals(LocalDate.of(2027, 2, 10), prestamo.getCronograma().obtener(2).getFechaVencimiento());
    }

    @Test
    void conTodosLosDiasLaCuotaPuedeVencerEnDomingo() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);

        prestamo.actualizarCronograma(LocalDate.of(2026, 12, 11));

        assertEquals(LocalDate.of(2027, 1, 10), prestamo.getCronograma().obtener(1).getFechaVencimiento());
    }

    // Caso 2

    /** Caso 2: paga la cuota del 10 dic y el 10 ene paga 660.000. */
    private static Prestamo prestamoCaso2(BaseInteres base) {
        Prestamo prestamo = prestamoCaso1(base);
        prestamo.registrarPago(LocalDate.of(2026, 12, 10), pesos("160000"));
        prestamo.registrarPago(LocalDate.of(2027, 1, 10), pesos("660000"));
        return prestamo;
    }

    @Test
    void caso2ElPagoDe660000Va160000ACuotasY500000ACapital() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);
        prestamo.registrarPago(LocalDate.of(2026, 12, 10), pesos("160000"));

        Pago pago = prestamo.registrarPago(LocalDate.of(2027, 1, 10), pesos("660000"));

        assertEquals(pesos("160000"), pago.getAbonoACuotas());
        assertEquals(pesos("500000"), pago.getAbonoACapital());
        assertEquals(pesos("1500000"), prestamo.getCapitalPendiente());
    }

    @Test
    void caso2ConCapitalInicialLasCuotasSiguientesSiguenEn160000() {
        Prestamo prestamo = prestamoCaso2(BaseInteres.CAPITAL_INICIAL);

        prestamo.actualizarCronograma(LocalDate.of(2027, 2, 11));

        assertEquals(pesos("160000"), prestamo.getCronograma().obtener(2).getValor());
        assertEquals(pesos("160000"), prestamo.getCronograma().obtener(3).getValor());
        assertEquals(pesos("1500000"), prestamo.getCapitalPendiente());
    }

    @Test
    void caso2ConCapitalVigenteLasCuotasSiguientesBajanA120000() {
        Prestamo prestamo = prestamoCaso2(BaseInteres.CAPITAL_VIGENTE);

        prestamo.actualizarCronograma(LocalDate.of(2027, 2, 11));

        assertEquals(pesos("160000"), prestamo.getCronograma().obtener(1).getValor());
        assertEquals(pesos("120000"), prestamo.getCronograma().obtener(2).getValor());
        assertEquals(pesos("120000"), prestamo.getCronograma().obtener(3).getValor());
    }

    // Caso 3

    @Test
    void caso3UnPagoDe100000DejaLaCuotaConSaldoYEnMoraDespuesDelVencimiento() {
        Prestamo prestamo = prestamoCaso2(BaseInteres.CAPITAL_INICIAL);

        Pago pago = prestamo.registrarPago(LocalDate.of(2027, 2, 10), pesos("100000"));

        Cuota febrero = prestamo.getCronograma().obtener(2);
        assertEquals(LocalDate.of(2027, 2, 10), febrero.getFechaVencimiento());
        assertEquals(pesos("60000"), febrero.saldoPendiente());
        assertEquals(EstadoCuota.CON_TIEMPO, febrero.estado(LocalDate.of(2027, 2, 10)));
        assertEquals(EstadoCuota.MORA, febrero.estado(LocalDate.of(2027, 2, 11)));
        // Nada fue a capital.
        assertEquals(pesos("0"), pago.getAbonoACapital());
        assertEquals(pesos("1500000"), prestamo.getCapitalPendiente());
    }

    // Imputación

    @Test
    void elPagoCubreLasVencidasDeLaMasViejaALaMasNuevaYLuegoLaActual() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);

        // El 15 ene están vencidas las del 10 dic y 10 ene, y la actual es la del 10 feb.
        Pago pago = prestamo.registrarPago(LocalDate.of(2027, 1, 15), pesos("400000"));

        assertTrue(prestamo.getCronograma().obtener(0).estaCubierta());
        assertTrue(prestamo.getCronograma().obtener(1).estaCubierta());
        assertEquals(pesos("80000"), prestamo.getCronograma().obtener(2).getValorCubierto());
        assertEquals(pesos("400000"), pago.getAbonoACuotas());
        assertEquals(pesos("2000000"), prestamo.getCapitalPendiente());
    }

    @Test
    void losPagosQuedanEnLaPilaConElUltimoArriba() {
        Prestamo prestamo = prestamoCaso2(BaseInteres.CAPITAL_INICIAL);

        assertEquals(2, prestamo.getPagos().tamano());
        assertEquals(LocalDate.of(2027, 1, 10), prestamo.getPagos().verTope().getFecha());
    }

    // Cierre

    @Test
    void alPagarTodoElPrestamoSeCierraYNoGeneraMasCuotas() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);
        assertEquals(pesos("2160000"), prestamo.saldoTotal());

        prestamo.registrarPago(LocalDate.of(2026, 12, 10), pesos("2160000"));

        assertTrue(prestamo.estaCerrado());
        assertEquals(pesos("0"), prestamo.saldoTotal());
        prestamo.actualizarCronograma(LocalDate.of(2027, 3, 1));
        assertEquals(1, prestamo.getCronograma().tamano());
    }

    @Test
    void unPrestamoCerradoNoRecibePagos() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);
        prestamo.registrarPago(LocalDate.of(2026, 12, 10), pesos("2160000"));

        assertThrows(IllegalStateException.class,
                () -> prestamo.registrarPago(LocalDate.of(2026, 12, 11), pesos("1000")));
    }

    @Test
    void unPagoMayorQueLaDeudaSeRechazaSinCambiarNada() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);

        assertThrows(IllegalArgumentException.class,
                () -> prestamo.registrarPago(LocalDate.of(2026, 12, 10), pesos("2160001")));
        assertEquals(pesos("2000000"), prestamo.getCapitalPendiente());
        assertEquals(pesos("0"), prestamo.getCronograma().primero().getValorCubierto());
        assertTrue(prestamo.getPagos().estaVacia());
    }

    @Test
    void unPagoConFechaAnteriorAlUltimoPagoSeRechaza() {
        Prestamo prestamo = prestamoCaso2(BaseInteres.CAPITAL_INICIAL);

        assertThrows(IllegalArgumentException.class,
                () -> prestamo.registrarPago(LocalDate.of(2027, 1, 9), pesos("1000")));
    }

    @Test
    void unPagoAntesDelDesembolsoOConMontoNoPositivoSeRechaza() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);

        assertThrows(IllegalArgumentException.class,
                () -> prestamo.registrarPago(LocalDate.of(2026, 11, 9), pesos("1000")));
        assertThrows(IllegalArgumentException.class,
                () -> prestamo.registrarPago(LocalDate.of(2026, 12, 1), BigDecimal.ZERO));
    }

    // Redondeo

    @Test
    void elInteresSeRedondeaAlPesoHaciaArribaDesdeLaMitad() {
        // 150.050 × 1 % = 1.500,50 → 1.501
        Contrato contrato = Contrato.borrador(pesos("150050"), pesos("1"), DESEMBOLSO_CASO_1).firmar();

        assertEquals(pesos("1501"), new Prestamo("1003", contrato).getCronograma().primero().getValor());
    }

    @Test
    void elInteresSeRedondeaAlPesoHaciaAbajoAntesDeLaMitad() {
        // 1.234.567 × 3 % = 37.037,01 → 37.037
        Contrato contrato = Contrato.borrador(pesos("1234567"), pesos("3"), DESEMBOLSO_CASO_1).firmar();

        assertEquals(pesos("37037"), new Prestamo("1004", contrato).getCronograma().primero().getValor());
    }

    // Validaciones del préstamo

    @Test
    void prestamoSinCedulaLanzaExcepcion() {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO_CASO_1).firmar();

        assertThrows(IllegalArgumentException.class, () -> new Prestamo(" ", contrato));
    }
}
