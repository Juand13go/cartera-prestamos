package io.github.juand13go.cartera.adaptadores.persistencia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;

import io.github.juand13go.cartera.dominio.politicas.Frecuencia;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaMensual;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaQuincenal;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaSemanal;
import io.github.juand13go.cartera.dominio.politicas.MultaFijaDiaria;
import io.github.juand13go.cartera.dominio.politicas.PoliticaMora;
import io.github.juand13go.cartera.dominio.politicas.PorcentajeDiario;
import io.github.juand13go.cartera.dominio.politicas.PorcentajeUnico;
import io.github.juand13go.cartera.dominio.politicas.SinMulta;

/**
 * Traduce las políticas del contrato (objetos) a columnas de la tabla prestamo, y de vuelta.
 * Los enums se guardan con su nombre (name()) y se leen con valueOf(); aquí solo van
 * las dos políticas que son clases: la frecuencia y la mora.
 */
final class ConversorContrato {

    private ConversorContrato() {
    }

    // Frecuencia ↔ columnas frecuencia + dia_cobro

    static String tipoFrecuencia(Frecuencia frecuencia) {
        if (frecuencia instanceof FrecuenciaMensual) {
            return "MENSUAL";
        }
        if (frecuencia instanceof FrecuenciaSemanal) {
            return "SEMANAL";
        }
        if (frecuencia instanceof FrecuenciaQuincenal) {
            return "QUINCENAL";
        }
        throw new IllegalArgumentException("Frecuencia desconocida: " + frecuencia);
    }

    /** Mensual: día del mes. Semanal: día de la semana (1 = lunes ... 7 = domingo). Quincenal: null. */
    static Integer diaDeCobro(Frecuencia frecuencia) {
        if (frecuencia instanceof FrecuenciaMensual mensual) {
            return mensual.getDiaDeCobro();
        }
        if (frecuencia instanceof FrecuenciaSemanal semanal) {
            return semanal.getDiaDeCobro().getValue();
        }
        return null;
    }

    static Frecuencia frecuencia(String tipo, Integer diaDeCobro) {
        return switch (tipo) {
            case "MENSUAL" -> new FrecuenciaMensual(diaDeCobro);
            case "SEMANAL" -> new FrecuenciaSemanal(DayOfWeek.of(diaDeCobro));
            case "QUINCENAL" -> new FrecuenciaQuincenal();
            default -> throw new IllegalArgumentException("Frecuencia desconocida: " + tipo);
        };
    }

    // Política de mora ↔ columnas politica_mora + valor_mora

    static String tipoMora(PoliticaMora politica) {
        if (politica instanceof SinMulta) {
            return "SIN_MULTA";
        }
        if (politica instanceof MultaFijaDiaria) {
            return "MULTA_FIJA_DIARIA";
        }
        if (politica instanceof PorcentajeUnico) {
            return "PORCENTAJE_UNICO";
        }
        if (politica instanceof PorcentajeDiario) {
            return "PORCENTAJE_DIARIO";
        }
        throw new IllegalArgumentException("Política de mora desconocida: " + politica);
    }

    /** Valor diario o porcentaje, según la política; null para SinMulta. */
    static BigDecimal valorMora(PoliticaMora politica) {
        if (politica instanceof MultaFijaDiaria fija) {
            return fija.getValorDiario();
        }
        if (politica instanceof PorcentajeUnico unico) {
            return unico.getPorcentaje();
        }
        if (politica instanceof PorcentajeDiario diario) {
            return diario.getPorcentaje();
        }
        return null;
    }

    /**
     * La columna valor_mora tiene 4 decimales (para porcentajes como 1,5 %).
     * La multa fija es dinero, así que se lee redondeada al peso.
     */
    static PoliticaMora politicaMora(String tipo, BigDecimal valor) {
        return switch (tipo) {
            case "SIN_MULTA" -> new SinMulta();
            case "MULTA_FIJA_DIARIA" -> new MultaFijaDiaria(valor.setScale(0, RoundingMode.HALF_UP));
            case "PORCENTAJE_UNICO" -> new PorcentajeUnico(valor);
            case "PORCENTAJE_DIARIO" -> new PorcentajeDiario(valor);
            default -> throw new IllegalArgumentException("Política de mora desconocida: " + tipo);
        };
    }
}
