package io.github.juand13go.cartera.adaptadores.web;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.modelo.EstadoCuota;
import io.github.juand13go.cartera.dominio.modelo.Pago;
import io.github.juand13go.cartera.dominio.modelo.Recibo;
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

    /** "1, 2" o "Ninguna" si el pago no completó ninguna cuota. */
    public String cuotas(ListaDoble<Integer> numeros) {
        if (numeros.estaVacia()) {
            return "Ninguna";
        }
        StringBuilder texto = new StringBuilder();
        for (Integer numero : numeros) {
            if (texto.length() > 0) {
                texto.append(", ");
            }
            texto.append(numero);
        }
        return texto.toString();
    }

    /** El recibo como texto plano, para enviarlo por WhatsApp. */
    public String textoRecibo(Recibo recibo) {
        Pago pago = recibo.getPago();
        return "Recibo N° " + recibo.getNumero() + " - Préstamo #" + recibo.getPrestamoId() + "\n"
                + "Cliente: " + recibo.getCliente().getNombre() + "\n"
                + "Fecha: " + recibo.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n"
                + "Valor pagado: " + pesos(recibo.getValor()) + "\n"
                + "A multas: " + pesos(pago.getAbonoAMultas())
                + " | A cuotas: " + pesos(pago.getAbonoACuotas())
                + " | A capital: " + pesos(pago.getAbonoACapital()) + "\n"
                + "Cuotas cubiertas: " + cuotas(recibo.getCuotasCubiertas()) + "\n"
                + "Saldo restante: " + pesos(recibo.getSaldoRestante());
    }

    /**
     * https://wa.me/57<celular>?text=<recibo>. El texto se codifica para URL (espacios, tildes y saltos de línea
     * se vuelven códigos como %20). URLEncoder pone "+" en los espacios; se cambian por %20, que WhatsApp entiende siempre.
     */
    public String enlaceWhatsapp(Recibo recibo) {
        String texto = URLEncoder.encode(textoRecibo(recibo), StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/57" + recibo.getCliente().getTelefono() + "?text=" + texto;
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
