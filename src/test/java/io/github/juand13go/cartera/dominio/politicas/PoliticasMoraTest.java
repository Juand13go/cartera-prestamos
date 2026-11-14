package io.github.juand13go.cartera.dominio.politicas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/** Las 4 políticas de mora con una cuota de $160.000. Aquí ya no hay días de gracia: llegan descontados. */
class PoliticasMoraTest {

    private static final BigDecimal CUOTA = new BigDecimal("160000");

    private static BigDecimal pesos(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void sinMultaNuncaCobra() {
        PoliticaMora politica = new SinMulta();

        assertEquals(BigDecimal.ZERO, politica.multa(CUOTA, 0));
        assertEquals(BigDecimal.ZERO, politica.multa(CUOTA, 30));
    }

    @Test
    void multaFijaDiariaCobraElValorPorCadaDia() {
        PoliticaMora politica = new MultaFijaDiaria(pesos("2000"));

        assertEquals(0, politica.multa(CUOTA, 0).compareTo(BigDecimal.ZERO));
        assertEquals(pesos("6000"), politica.multa(CUOTA, 3));
    }

    @Test
    void porcentajeUnicoCobraUnaSolaVezSinImportarLosDias() {
        PoliticaMora politica = new PorcentajeUnico(pesos("10"));

        assertEquals(BigDecimal.ZERO, politica.multa(CUOTA, 0));
        assertEquals(pesos("16000"), politica.multa(CUOTA, 1));
        assertEquals(pesos("16000"), politica.multa(CUOTA, 20));
    }

    @Test
    void porcentajeDiarioCobraElPorcentajePorCadaDia() {
        PoliticaMora politica = new PorcentajeDiario(pesos("1"));

        assertEquals(0, politica.multa(CUOTA, 0).compareTo(BigDecimal.ZERO));
        assertEquals(pesos("1600"), politica.multa(CUOTA, 1));
        assertEquals(pesos("4800"), politica.multa(CUOTA, 3));
    }

    @Test
    void porcentajeDiarioRedondeaUnaSolaVezAlFinal() {
        // 1.001 × 0,5 % = 5,005 por día. En 100 días: 500,5 → 501.
        // Si se redondeara cada día (5 × 100) darían 500: se perdería un peso.
        PoliticaMora politica = new PorcentajeDiario(pesos("0.5"));

        assertEquals(pesos("501"), politica.multa(pesos("1001"), 100));
    }

    @Test
    void valoresYPorcentajesNoPositivosLanzanExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new MultaFijaDiaria(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new PorcentajeUnico(pesos("-1")));
        assertThrows(IllegalArgumentException.class, () -> new PorcentajeDiario(null));
    }
}
