package io.github.juand13go.cartera.adaptadores.web;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;

import org.springframework.stereotype.Component;

import io.github.juand13go.cartera.dominio.modelo.EstadoCuota;
import io.github.juand13go.cartera.dominio.politicas.Frecuencia;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaMensual;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaSemanal;
import io.github.juand13go.cartera.dominio.politicas.MultaFijaDiaria;
import io.github.juand13go.cartera.dominio.politicas.PoliticaMora;
import io.github.juand13go.cartera.dominio.politicas.PorcentajeDiario;
import io.github.juand13go.cartera.dominio.politicas.PorcentajeUnico;

/**
 * Convierte objetos del dominio en frases para mostrar. Las plantillas lo usan como ${@textos.frecuencia(...)}.
 * Así la vista no tiene que preguntar de qué clase es cada política.
 */
@Component("textos")
public class Textos {

    /** $1.234.567 (pesos sin decimales, con punto de miles). */
    public static String pesos(BigDecimal valor) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setGroupingSeparator('.');
        return "$" + new DecimalFormat("#,##0", simbolos).format(valor);
    }

    public String frecuencia(Frecuencia frecuencia) {
        if (frecuencia instanceof FrecuenciaMensual mensual) {
            return "Mensual, el día " + mensual.getDiaDeCobro();
        }
        if (frecuencia instanceof FrecuenciaSemanal semanal) {
            return "Semanal, los " + diaDeLaSemana(semanal.getDiaDeCobro());
        }
        return "Quincenal (día 15 y último día del mes)";
    }

    public String mora(PoliticaMora politica) {
        if (politica instanceof MultaFijaDiaria fija) {
            return pesos(fija.getValorDiario()) + " por día de atraso";
        }
        if (politica instanceof PorcentajeUnico unico) {
            return unico.getPorcentaje().stripTrailingZeros().toPlainString() + " % de la cuota, una sola vez";
        }
        if (politica instanceof PorcentajeDiario diario) {
            return diario.getPorcentaje().stripTrailingZeros().toPlainString() + " % de la cuota por día de atraso";
        }
        return "Sin multa";
    }

    public String estado(EstadoCuota estado) {
        return switch (estado) {
            case CON_TIEMPO -> "Con tiempo";
            case PAGADA -> "Pagada";
            case PAGADA_CON_RETRASO -> "Pagada con retraso";
            case MORA -> "En mora";
        };
    }

    private static String diaDeLaSemana(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> "lunes";
            case TUESDAY -> "martes";
            case WEDNESDAY -> "miércoles";
            case THURSDAY -> "jueves";
            case FRIDAY -> "viernes";
            case SATURDAY -> "sábados";
            case SUNDAY -> "domingos";
        };
    }
}
