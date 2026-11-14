package io.github.juand13go.cartera.adaptadores.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;
import io.github.juand13go.cartera.dominio.politicas.FrecuenciaSemanal;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CrearPrestamoWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RepositorioPrestamos prestamos;

    /**
     * Un formulario completo con valores válidos. Cada prueba cambia solo lo que le interesa con pares
     * campo-valor que REEMPLAZAN el valor base (MockMvc agregaría un segundo valor si se llamara param dos veces).
     */
    private static MockHttpServletRequestBuilder formulario(String cedula, String capital, String... cambios) {
        String[][] campos = {
                {"cedula", cedula}, {"capital", capital}, {"tasa", "8"}, {"baseInteres", "CAPITAL_INICIAL"},
                {"fechaDesembolso", "2026-11-14"}, {"frecuencia", "MENSUAL"}, {"diaMes", "14"},
                {"diaSemana", "SATURDAY"}, {"calendario", "TODOS_LOS_DIAS"}, {"politicaMora", "SIN_MULTA"},
                {"diasGracia", "2"}, {"ordenImputacion", "MULTAS_PRIMERO"}, {"alVencerPlazo", "TRATAR_COMO_MORA"},
                {"interesCancelacion", "PERIODO_COMPLETO"}, {"confirmaSobreCupo", "false"}};
        for (int i = 0; i < cambios.length; i += 2) {
            for (String[] campo : campos) {
                if (campo[0].equals(cambios[i])) {
                    campo[1] = cambios[i + 1];
                }
            }
        }
        MockHttpServletRequestBuilder peticion = post("/prestamos");
        for (String[] campo : campos) {
            peticion.param(campo[0], campo[1]);
        }
        return peticion;
    }

    @Test
    void elFormularioTraeLosValoresPorDefectoDelContrato() throws Exception {
        mvc.perform(get("/prestamos/nuevo").param("cedula", "1000000004"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nuevo préstamo para")))
                .andExpect(content().string(containsString("Jorge Castaño (prueba)")))
                .andExpect(content().string(containsString("value=\"CAPITAL_INICIAL\" selected")))
                .andExpect(content().string(containsString("value=\"MULTAS_PRIMERO\" selected")))
                .andExpect(content().string(containsString("value=\"SIN_MULTA\" selected")));
    }

    @Test
    void conUnClienteQueNoExisteVuelveALaListaDeClientes() throws Exception {
        mvc.perform(get("/prestamos/nuevo").param("cedula", "1234567"))
                .andExpect(redirectedUrl("/clientes"));
    }

    @Test
    void crearUnPrestamoDentroDelCupoLlevaAlDetalle() throws Exception {
        // Jorge tiene cupo de 2.000.000 y su único préstamo está cerrado.
        mvc.perform(formulario("1000000004", "1000000"))
                .andExpect(redirectedUrlPattern("/prestamos/*"));
    }

    @Test
    void siSuperaElCupoPideConfirmacion() throws Exception {
        // Laura tiene cupo de 5.000.000 y ya debe 2.300.000.
        mvc.perform(formulario("1000000001", "3000000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Este préstamo supera el cupo del cliente")))
                .andExpect(content().string(containsString("$2.300.000")))
                .andExpect(content().string(containsString("Confirmo que quiero prestar")));
    }

    @Test
    void conLaConfirmacionSeCreaAunqueSupereElCupo() throws Exception {
        mvc.perform(formulario("1000000001", "3000000", "confirmaSobreCupo", "true"))
                .andExpect(redirectedUrlPattern("/prestamos/*"));
    }

    @Test
    void unCapitalMenorQueElMinimoSeRechazaConSuMotivo() throws Exception {
        mvc.perform(formulario("1000000004", "10000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El capital mínimo es 50000")));
    }

    @Test
    void unaMultaSinValorSeRechaza() throws Exception {
        mvc.perform(formulario("1000000004", "1000000", "politicaMora", "MULTA_FIJA_DIARIA"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escriba el valor o el porcentaje de la multa")));
    }

    @Test
    void unaFechaMalEscritaSeAvisa() throws Exception {
        mvc.perform(formulario("1000000004", "1000000", "fechaDesembolso", "14/11/2026"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Revise los campos")));
    }

    @Test
    void lasCondicionesElegidasQuedanEnElContratoGuardado() throws Exception {
        String destino = mvc.perform(formulario("1000000004", "1000000",
                        "frecuencia", "SEMANAL", "diaSemana", "FRIDAY"))
                .andReturn().getResponse().getRedirectedUrl();

        long id = Long.parseLong(destino.substring(destino.lastIndexOf('/') + 1));
        Prestamo guardado = prestamos.buscarPorId(id);
        assertTrue(guardado.getContrato().getFrecuencia() instanceof FrecuenciaSemanal);
        assertEquals(DayOfWeek.FRIDAY, ((FrecuenciaSemanal) guardado.getContrato().getFrecuencia()).getDiaDeCobro());
    }
}
