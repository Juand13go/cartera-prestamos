package io.github.juand13go.cartera.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.politicas.Calendario;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaMensual;
import io.github.juand13go.cartera.dominio.politicas.MultaFijaDiaria;
import io.github.juand13go.cartera.dominio.politicas.OrdenImputacion;

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

    // Multas e imputación: caso 1 con multa fija de $2.000 diarios y 2 días de gracia.
    // Si no paga el 10 dic, el 15 dic la cuota 1 lleva 5 días de atraso − 2 de gracia = 3 días → $6.000.

    private static Prestamo prestamoConMulta(OrdenImputacion orden) {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO_CASO_1)
                .mora(new MultaFijaDiaria(pesos("2000")))
                .ordenImputacion(orden)
                .firmar();
        return new Prestamo("1006", contrato);
    }

    @Test
    void elSaldoTotalIncluyeLasMultas() {
        Prestamo prestamo = prestamoConMulta(OrdenImputacion.MULTAS_PRIMERO);
        LocalDate quinceDic = LocalDate.of(2026, 12, 15);
        prestamo.actualizarCronograma(quinceDic);

        assertEquals(pesos("6000"), prestamo.saldoMulta(prestamo.getCronograma().primero(), quinceDic));
        // 2.000.000 de capital + 160.000 + 160.000 de cuotas + 6.000 de multa.
        assertEquals(pesos("2326000"), prestamo.saldoTotal(quinceDic));
    }

    @Test
    void multasPrimeroPagaLaMultaAntesQueLasCuotas() {
        Prestamo prestamo = prestamoConMulta(OrdenImputacion.MULTAS_PRIMERO);

        Pago pago = prestamo.registrarPago(LocalDate.of(2026, 12, 15), pesos("170000"));

        assertEquals(pesos("6000"), pago.getAbonoAMultas());
        assertEquals(pesos("164000"), pago.getAbonoACuotas());
        assertEquals(pesos("0"), pago.getAbonoACapital());
        assertTrue(prestamo.getCronograma().obtener(0).estaCubierta());
        assertEquals(pesos("4000"), prestamo.getCronograma().obtener(1).getValorCubierto());
    }

    @Test
    void cuotasPrimeroPagaLasCuotasYDejaLaMultaPendiente() {
        Prestamo prestamo = prestamoConMulta(OrdenImputacion.CUOTAS_PRIMERO);

        Pago pago = prestamo.registrarPago(LocalDate.of(2026, 12, 15), pesos("170000"));

        assertEquals(pesos("0"), pago.getAbonoAMultas());
        assertEquals(pesos("170000"), pago.getAbonoACuotas());
        assertEquals(pesos("10000"), prestamo.getCronograma().obtener(1).getValorCubierto());
        // La cuota 1 quedó cubierta el 15 dic: su multa se congela en 6.000 y sigue pendiente.
        Cuota primera = prestamo.getCronograma().primero();
        assertEquals(pesos("6000"), prestamo.saldoMulta(primera, LocalDate.of(2027, 1, 1)));
    }

    @Test
    void cuotasPrimeroPagaLasMultasAntesQueElCapital() {
        Prestamo prestamo = prestamoConMulta(OrdenImputacion.CUOTAS_PRIMERO);

        Pago pago = prestamo.registrarPago(LocalDate.of(2026, 12, 15), pesos("2326000"));

        assertEquals(pesos("320000"), pago.getAbonoACuotas());
        assertEquals(pesos("6000"), pago.getAbonoAMultas());
        assertEquals(pesos("2000000"), pago.getAbonoACapital());
        assertTrue(prestamo.estaCerrado());
    }

    @Test
    void unAbonoParcialALaMultaSeDescuentaPeroLaMultaSigueCreciendo() {
        Prestamo prestamo = prestamoConMulta(OrdenImputacion.MULTAS_PRIMERO);

        prestamo.registrarPago(LocalDate.of(2026, 12, 15), pesos("4000"));

        Cuota primera = prestamo.getCronograma().primero();
        assertEquals(pesos("4000"), primera.getMultaPagada());
        assertEquals(pesos("2000"), prestamo.saldoMulta(primera, LocalDate.of(2026, 12, 15)));
        // Al día siguiente la multa total ya es 8.000 (4 días); faltan 4.000.
        assertEquals(pesos("4000"), prestamo.saldoMulta(primera, LocalDate.of(2026, 12, 16)));
    }

    @Test
    void lasMultasSePaganDeLaMasViejaALaMasNueva() {
        Prestamo prestamo = prestamoConMulta(OrdenImputacion.MULTAS_PRIMERO);

        // El 15 ene: cuota 1 con 36 días de atraso (34 con multa → 68.000)
        // y cuota 2 con 5 días (3 con multa → 6.000).
        Pago pago = prestamo.registrarPago(LocalDate.of(2027, 1, 15), pesos("70000"));

        assertEquals(pesos("70000"), pago.getAbonoAMultas());
        assertEquals(pesos("68000"), prestamo.getCronograma().obtener(0).getMultaPagada());
        assertEquals(pesos("2000"), prestamo.getCronograma().obtener(1).getMultaPagada());
    }

    // Condonación (caso 1 con multa de $2.000 diarios; el 15 dic la cuota 1 debe $6.000 de multa)

    @Test
    void condonarAntesDePagarHaceQueTodoElPagoVayaACuotas() {
        Prestamo prestamo = prestamoConMulta(OrdenImputacion.MULTAS_PRIMERO);
        LocalDate quinceDic = LocalDate.of(2026, 12, 15);

        prestamo.condonarMulta(1, quinceDic);

        assertEquals(pesos("0"), prestamo.saldoMulta(prestamo.getCronograma().primero(), quinceDic));
        Pago pago = prestamo.registrarPago(quinceDic, pesos("170000"));
        assertEquals(pesos("0"), pago.getAbonoAMultas());
        assertEquals(pesos("170000"), pago.getAbonoACuotas());
    }

    @Test
    void unaMultaCondonadaNoVuelveACrecer() {
        Prestamo prestamo = prestamoConMulta(OrdenImputacion.MULTAS_PRIMERO);

        prestamo.condonarMulta(1, LocalDate.of(2026, 12, 15));

        assertEquals(pesos("0"), prestamo.saldoMulta(prestamo.getCronograma().primero(), LocalDate.of(2027, 1, 20)));
    }

    @Test
    void condonarRespetaLoQueYaSeHabiaPagadoDeLaMulta() {
        Prestamo prestamo = prestamoConMulta(OrdenImputacion.MULTAS_PRIMERO);
        prestamo.registrarPago(LocalDate.of(2026, 12, 15), pesos("4000"));

        prestamo.condonarMulta(1, LocalDate.of(2026, 12, 15));

        Cuota primera = prestamo.getCronograma().primero();
        assertEquals(pesos("4000"), primera.getMultaPagada());
        assertEquals(pesos("0"), prestamo.saldoMulta(primera, LocalDate.of(2026, 12, 20)));
    }

    @Test
    void laCondonacionSobreviveAlDeshacerUnPagoPosterior() {
        Prestamo prestamo = prestamoConMulta(OrdenImputacion.MULTAS_PRIMERO);
        prestamo.condonarMulta(1, LocalDate.of(2026, 12, 15));
        prestamo.registrarPago(LocalDate.of(2026, 12, 20), pesos("170000"));

        prestamo.deshacerUltimoPago();

        Cuota primera = prestamo.getCronograma().primero();
        assertTrue(primera.estaMultaCondonada());
        assertEquals(pesos("0"), prestamo.saldoMulta(primera, LocalDate.of(2026, 12, 20)));
    }

    @Test
    void deshacerUnPagoHechoAntesDeCondonarVuelveACobrarLoQueEsePagoHabiaCubierto() {
        // Se condonó "lo que faltaba" cuando ya había 4.000 pagados. Si se anula ese pago,
        // los 4.000 vuelven a deberse: la condonación nunca los perdonó.
        Prestamo prestamo = prestamoConMulta(OrdenImputacion.MULTAS_PRIMERO);
        prestamo.registrarPago(LocalDate.of(2026, 12, 15), pesos("4000"));
        prestamo.condonarMulta(1, LocalDate.of(2026, 12, 15));

        prestamo.deshacerUltimoPago();
        prestamo.actualizarCronograma(LocalDate.of(2026, 12, 20));

        assertEquals(pesos("4000"), prestamo.saldoMulta(prestamo.getCronograma().primero(), LocalDate.of(2026, 12, 20)));
    }

    @Test
    void condonarUnaCuotaQueNoExisteOYaCondonadaLanzaExcepcion() {
        Prestamo prestamo = prestamoConMulta(OrdenImputacion.MULTAS_PRIMERO);
        LocalDate quinceDic = LocalDate.of(2026, 12, 15);

        assertThrows(IllegalArgumentException.class, () -> prestamo.condonarMulta(0, quinceDic));
        assertThrows(IllegalArgumentException.class, () -> prestamo.condonarMulta(3, quinceDic));
        prestamo.condonarMulta(1, quinceDic);
        assertThrows(IllegalStateException.class, () -> prestamo.condonarMulta(1, quinceDic));
    }

    // Deshacer

    @Test
    void deshacerElPagoDe660000DevuelveElCapitalYDejaLaCuotaPendiente() {
        Prestamo prestamo = prestamoCaso2(BaseInteres.CAPITAL_INICIAL);

        Pago deshecho = prestamo.deshacerUltimoPago();

        assertEquals(pesos("660000"), deshecho.getMonto());
        assertEquals(pesos("2000000"), prestamo.getCapitalPendiente());
        assertEquals(1, prestamo.getPagos().tamano());
        prestamo.actualizarCronograma(LocalDate.of(2027, 1, 10));
        assertTrue(prestamo.getCronograma().obtener(0).estaCubierta());
        assertEquals(pesos("160000"), prestamo.getCronograma().obtener(1).saldoPendiente());
    }

    @Test
    void conCapitalVigenteDeshacerHaceQueLasCuotasVuelvanA160000() {
        Prestamo prestamo = prestamoCaso2(BaseInteres.CAPITAL_VIGENTE);
        prestamo.actualizarCronograma(LocalDate.of(2027, 2, 11));
        assertEquals(pesos("120000"), prestamo.getCronograma().obtener(2).getValor());

        prestamo.deshacerUltimoPago();
        prestamo.actualizarCronograma(LocalDate.of(2027, 2, 11));

        assertEquals(pesos("160000"), prestamo.getCronograma().obtener(2).getValor());
        assertEquals(pesos("160000"), prestamo.getCronograma().obtener(3).getValor());
    }

    @Test
    void deshacerTodosLosPagosDejaElPrestamoComoAlDesembolsar() {
        Prestamo prestamo = prestamoCaso2(BaseInteres.CAPITAL_INICIAL);

        prestamo.deshacerUltimoPago();
        prestamo.deshacerUltimoPago();

        assertTrue(prestamo.getPagos().estaVacia());
        assertEquals(pesos("2000000"), prestamo.getCapitalPendiente());
        assertEquals(1, prestamo.getCronograma().tamano());
        assertEquals(pesos("0"), prestamo.getCronograma().primero().getValorCubierto());
    }

    @Test
    void deshacerElPagoQueCerroElPrestamoLoReabre() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);
        prestamo.registrarPago(LocalDate.of(2026, 12, 10), pesos("2160000"));

        prestamo.deshacerUltimoPago();

        assertFalse(prestamo.estaCerrado());
        prestamo.registrarPago(LocalDate.of(2026, 12, 10), pesos("160000"));
        assertEquals(pesos("2000000"), prestamo.getCapitalPendiente());
    }

    @Test
    void deshacerRespetaLasMultasPagadasPorLosPagosQueQuedan() {
        Prestamo prestamo = prestamoConMulta(OrdenImputacion.MULTAS_PRIMERO);
        prestamo.registrarPago(LocalDate.of(2026, 12, 15), pesos("4000"));
        prestamo.registrarPago(LocalDate.of(2026, 12, 20), pesos("170000"));

        prestamo.deshacerUltimoPago();

        Cuota primera = prestamo.getCronograma().primero();
        assertEquals(pesos("4000"), primera.getMultaPagada());
        assertEquals(pesos("0"), primera.getValorCubierto());
    }

    @Test
    void deshacerSinPagosLanzaExcepcion() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);

        assertThrows(IllegalStateException.class, prestamo::deshacerUltimoPago);
    }

    // Cierre

    @Test
    void alPagarTodoElPrestamoSeCierraYNoGeneraMasCuotas() {
        Prestamo prestamo = prestamoCaso1(BaseInteres.CAPITAL_INICIAL);
        assertEquals(pesos("2160000"), prestamo.saldoTotal(LocalDate.of(2026, 12, 10)));

        prestamo.registrarPago(LocalDate.of(2026, 12, 10), pesos("2160000"));

        assertTrue(prestamo.estaCerrado());
        assertEquals(pesos("0"), prestamo.saldoTotal(LocalDate.of(2026, 12, 10)));
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
