package io.github.juand13go.cartera.adaptadores.web;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import io.github.juand13go.cartera.dominio.modelo.BaseInteres;
import io.github.juand13go.cartera.dominio.modelo.Contrato;
import io.github.juand13go.cartera.dominio.politicas.AlVencerPlazo;
import io.github.juand13go.cartera.dominio.politicas.Calendario;
import io.github.juand13go.cartera.dominio.politicas.Frecuencia;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaMensual;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaQuincenal;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaSemanal;
import io.github.juand13go.cartera.dominio.politicas.InteresCancelacion;
import io.github.juand13go.cartera.dominio.politicas.MultaFijaDiaria;
import io.github.juand13go.cartera.dominio.politicas.OrdenImputacion;
import io.github.juand13go.cartera.dominio.politicas.PoliticaMora;
import io.github.juand13go.cartera.dominio.politicas.PorcentajeDiario;
import io.github.juand13go.cartera.dominio.politicas.PorcentajeUnico;
import io.github.juand13go.cartera.dominio.politicas.SinMulta;

/** Los campos del formulario de préstamo nuevo: las 11 condiciones del contrato y la confirmación de cupo. */
public class FormularioPrestamo {
    private String cedula;
    private BigDecimal capital;
    private BigDecimal tasa;
    private BaseInteres baseInteres;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaDesembolso;
    private String frecuencia;
    private Integer diaMes;
    private DayOfWeek diaSemana;
    private Calendario calendario;
    private String politicaMora;
    private BigDecimal valorMora;
    private Integer diasGracia;
    private OrdenImputacion ordenImputacion;
    private AlVencerPlazo alVencerPlazo;
    private InteresCancelacion interesCancelacion;
    private boolean confirmaSobreCupo;

    /**
     * Formulario con los valores por defecto ya puestos. No se escriben aquí otra vez:
     * se firma un contrato de ejemplo con el borrador y se copian sus condiciones,
     * así los valores por defecto siguen viviendo en un solo lugar (Contrato.Borrador).
     */
    public static FormularioPrestamo conValoresPorDefecto(String cedula, LocalDate hoy) {
        Contrato ejemplo = Contrato.borrador(BigDecimal.ONE, BigDecimal.ONE, hoy).firmar();
        FormularioPrestamo formulario = new FormularioPrestamo();
        formulario.cedula = cedula;
        formulario.fechaDesembolso = hoy;
        formulario.baseInteres = ejemplo.getBaseInteres();
        formulario.calendario = ejemplo.getCalendario();
        formulario.diasGracia = ejemplo.getDiasDeGracia();
        formulario.ordenImputacion = ejemplo.getOrdenImputacion();
        formulario.alVencerPlazo = ejemplo.getAlVencerPlazo();
        formulario.interesCancelacion = ejemplo.getInteresCancelacion();
        formulario.politicaMora = ejemplo.getPoliticaMora() instanceof SinMulta ? "SIN_MULTA" : null;
        formulario.frecuencia = "MENSUAL";
        formulario.diaMes = ((FrecuenciaMensual) ejemplo.getFrecuencia()).getDiaDeCobro();
        formulario.diaSemana = hoy.getDayOfWeek();
        return formulario;
    }

    /** Arma el contrato con el borrador. Si un dato no sirve, el dominio (o este método) lanza IllegalArgumentException. */
    public Contrato aContrato() {
        if (diasGracia == null) {
            throw new IllegalArgumentException("Escriba los días de gracia");
        }
        return Contrato.borrador(capital, tasa, fechaDesembolso)
                .base(baseInteres)
                .frecuencia(armarFrecuencia())
                .calendario(calendario)
                .mora(armarPoliticaMora())
                .diasDeGracia(diasGracia)
                .ordenImputacion(ordenImputacion)
                .alVencerPlazo(alVencerPlazo)
                .interesCancelacion(interesCancelacion)
                .firmar();
    }

    private Frecuencia armarFrecuencia() {
        if ("MENSUAL".equals(frecuencia)) {
            if (diaMes == null) {
                throw new IllegalArgumentException("Escriba el día del mes en que se cobra");
            }
            return new FrecuenciaMensual(diaMes);
        }
        if ("SEMANAL".equals(frecuencia)) {
            return new FrecuenciaSemanal(diaSemana);
        }
        if ("QUINCENAL".equals(frecuencia)) {
            return new FrecuenciaQuincenal();
        }
        throw new IllegalArgumentException("Elija una frecuencia");
    }

    private PoliticaMora armarPoliticaMora() {
        if ("SIN_MULTA".equals(politicaMora)) {
            return new SinMulta();
        }
        if (valorMora == null) {
            throw new IllegalArgumentException("Escriba el valor o el porcentaje de la multa");
        }
        if ("MULTA_FIJA_DIARIA".equals(politicaMora)) {
            return new MultaFijaDiaria(valorMora);
        }
        if ("PORCENTAJE_UNICO".equals(politicaMora)) {
            return new PorcentajeUnico(valorMora);
        }
        if ("PORCENTAJE_DIARIO".equals(politicaMora)) {
            return new PorcentajeDiario(valorMora);
        }
        throw new IllegalArgumentException("Elija una política de mora");
    }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public BigDecimal getCapital() { return capital; }
    public void setCapital(BigDecimal capital) { this.capital = capital; }
    public BigDecimal getTasa() { return tasa; }
    public void setTasa(BigDecimal tasa) { this.tasa = tasa; }
    public BaseInteres getBaseInteres() { return baseInteres; }
    public void setBaseInteres(BaseInteres baseInteres) { this.baseInteres = baseInteres; }
    public LocalDate getFechaDesembolso() { return fechaDesembolso; }
    public void setFechaDesembolso(LocalDate fechaDesembolso) { this.fechaDesembolso = fechaDesembolso; }
    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }
    public Integer getDiaMes() { return diaMes; }
    public void setDiaMes(Integer diaMes) { this.diaMes = diaMes; }
    public DayOfWeek getDiaSemana() { return diaSemana; }
    public void setDiaSemana(DayOfWeek diaSemana) { this.diaSemana = diaSemana; }
    public Calendario getCalendario() { return calendario; }
    public void setCalendario(Calendario calendario) { this.calendario = calendario; }
    public String getPoliticaMora() { return politicaMora; }
    public void setPoliticaMora(String politicaMora) { this.politicaMora = politicaMora; }
    public BigDecimal getValorMora() { return valorMora; }
    public void setValorMora(BigDecimal valorMora) { this.valorMora = valorMora; }
    public Integer getDiasGracia() { return diasGracia; }
    public void setDiasGracia(Integer diasGracia) { this.diasGracia = diasGracia; }
    public OrdenImputacion getOrdenImputacion() { return ordenImputacion; }
    public void setOrdenImputacion(OrdenImputacion ordenImputacion) { this.ordenImputacion = ordenImputacion; }
    public AlVencerPlazo getAlVencerPlazo() { return alVencerPlazo; }
    public void setAlVencerPlazo(AlVencerPlazo alVencerPlazo) { this.alVencerPlazo = alVencerPlazo; }
    public InteresCancelacion getInteresCancelacion() { return interesCancelacion; }
    public void setInteresCancelacion(InteresCancelacion interesCancelacion) { this.interesCancelacion = interesCancelacion; }
    public boolean isConfirmaSobreCupo() { return confirmaSobreCupo; }
    public void setConfirmaSobreCupo(boolean confirmaSobreCupo) { this.confirmaSobreCupo = confirmaSobreCupo; }
}
