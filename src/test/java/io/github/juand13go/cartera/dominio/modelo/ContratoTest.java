package io.github.juand13go.cartera.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import io.github.juand13go.cartera.dominio.politicas.AlVencerPlazo;
import io.github.juand13go.cartera.dominio.politicas.Calendario;
import io.github.juand13go.cartera.dominio.politicas.Frecuencia;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaMensual;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaSemanal;
import io.github.juand13go.cartera.dominio.politicas.InteresCancelacion;
import io.github.juand13go.cartera.dominio.politicas.MultaFijaDiaria;
import io.github.juand13go.cartera.dominio.politicas.OrdenImputacion;
import io.github.juand13go.cartera.dominio.politicas.PoliticaMora;
import io.github.juand13go.cartera.dominio.politicas.PorcentajeDiario;
import io.github.juand13go.cartera.dominio.politicas.PorcentajeUnico;
import io.github.juand13go.cartera.dominio.politicas.SinMulta;

class ContratoTest {

    private static final LocalDate DESEMBOLSO = LocalDate.of(2026, 11, 10);

    private static BigDecimal pesos(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void unBorradorSinCambiosTraeLosValoresPorDefecto() {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).firmar();

        assertEquals(pesos("2000000"), contrato.getCapital());
        assertEquals(pesos("8"), contrato.getTasaPorPeriodo());
        assertEquals(DESEMBOLSO, contrato.getFechaDesembolso());
        assertEquals(BaseInteres.CAPITAL_INICIAL, contrato.getBaseInteres());
        assertEquals(Calendario.TODOS_LOS_DIAS, contrato.getCalendario());
        assertTrue(contrato.getPoliticaMora() instanceof SinMulta);
        assertEquals(2, contrato.getDiasDeGracia());
        assertEquals(OrdenImputacion.MULTAS_PRIMERO, contrato.getOrdenImputacion());
        assertEquals(AlVencerPlazo.TRATAR_COMO_MORA, contrato.getAlVencerPlazo());
        assertEquals(InteresCancelacion.PERIODO_COMPLETO, contrato.getInteresCancelacion());
    }

    @Test
    void laFrecuenciaPorDefectoEsMensualElDiaDelDesembolso() {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).firmar();

        assertTrue(contrato.getFrecuencia() instanceof FrecuenciaMensual);
        assertEquals(10, ((FrecuenciaMensual) contrato.getFrecuencia()).getDiaDeCobro());
    }

    @Test
    void elBorradorPermiteCambiarLasCondiciones() {
        Frecuencia semanal = new FrecuenciaSemanal(DayOfWeek.FRIDAY);

        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO)
                .base(BaseInteres.CAPITAL_VIGENTE)
                .frecuencia(semanal)
                .calendario(Calendario.SIN_DOMINGOS)
                .firmar();

        assertEquals(BaseInteres.CAPITAL_VIGENTE, contrato.getBaseInteres());
        assertSame(semanal, contrato.getFrecuencia());
        assertEquals(Calendario.SIN_DOMINGOS, contrato.getCalendario());
    }

    @Test
    void capitalOTasaNoPositivosLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(BigDecimal.ZERO, pesos("8"), DESEMBOLSO).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("-1"), DESEMBOLSO).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(null, pesos("8"), DESEMBOLSO).firmar());
    }

    @Test
    void sinFechaBaseOFrecuenciaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("8"), null).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).base(null).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).frecuencia(null).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).calendario(null).firmar());
    }

    @Test
    void diasDeGraciaNegativosOSinPoliticaDeMoraLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).diasDeGracia(-1).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).mora(null).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).ordenImputacion(null).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).alVencerPlazo(null).firmar());
        assertThrows(IllegalArgumentException.class,
                () -> Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).interesCancelacion(null).firmar());
    }

    // Multa con días de gracia: cuota de $160.000 que vence el 10 de febrero, 2 días de gracia

    private static Cuota cuotaDeFebrero() {
        return new Cuota(3, LocalDate.of(2027, 2, 10), pesos("160000"));
    }

    private static Contrato contratoConMora(PoliticaMora politica) {
        return Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).mora(politica).firmar();
    }

    @Test
    void sinMultaPorDefectoElAtrasoNoCuesta() {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO).firmar();

        assertEquals(BigDecimal.ZERO, contrato.multa(cuotaDeFebrero(), LocalDate.of(2027, 3, 1)));
    }

    @Test
    void duranteLosDiasDeGraciaNoHayMulta() {
        Contrato contrato = contratoConMora(new MultaFijaDiaria(pesos("2000")));

        assertEquals(0, contrato.multa(cuotaDeFebrero(), LocalDate.of(2027, 2, 12)).compareTo(BigDecimal.ZERO));
    }

    @Test
    void multaFijaDiariaCuentaDesdeQueTerminanLosDiasDeGracia() {
        Contrato contrato = contratoConMora(new MultaFijaDiaria(pesos("2000")));

        // 15 feb: 5 días de atraso − 2 de gracia = 3 días con multa.
        assertEquals(pesos("6000"), contrato.multa(cuotaDeFebrero(), LocalDate.of(2027, 2, 15)));
    }

    @Test
    void laMultaSeCongelaCuandoLaCuotaSeCubre() {
        Contrato contrato = contratoConMora(new MultaFijaDiaria(pesos("2000")));
        Cuota cuota = cuotaDeFebrero();
        cuota.cubrir(pesos("160000"), LocalDate.of(2027, 2, 14));

        // Cubierta el 14 feb: 4 días de atraso − 2 de gracia = 2 días, aunque se consulte el 1 mar.
        assertEquals(pesos("4000"), contrato.multa(cuota, LocalDate.of(2027, 3, 1)));
    }

    @Test
    void porcentajeUnicoSeCobraUnaVezAlTerminarLaGracia() {
        Contrato contrato = contratoConMora(new PorcentajeUnico(pesos("10")));

        assertEquals(pesos("16000"), contrato.multa(cuotaDeFebrero(), LocalDate.of(2027, 2, 13)));
        assertEquals(pesos("16000"), contrato.multa(cuotaDeFebrero(), LocalDate.of(2027, 3, 1)));
    }

    @Test
    void porcentajeDiarioCuentaCadaDiaDespuesDeLaGracia() {
        Contrato contrato = contratoConMora(new PorcentajeDiario(pesos("1")));

        assertEquals(pesos("4800"), contrato.multa(cuotaDeFebrero(), LocalDate.of(2027, 2, 15)));
    }

    @Test
    void conCeroDiasDeGraciaLaMultaEmpiezaAlDiaSiguienteDelVencimiento() {
        Contrato contrato = Contrato.borrador(pesos("2000000"), pesos("8"), DESEMBOLSO)
                .mora(new MultaFijaDiaria(pesos("2000")))
                .diasDeGracia(0)
                .firmar();

        assertEquals(pesos("2000"), contrato.multa(cuotaDeFebrero(), LocalDate.of(2027, 2, 11)));
    }
}
