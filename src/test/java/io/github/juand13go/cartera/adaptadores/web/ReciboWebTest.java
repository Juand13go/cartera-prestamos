package io.github.juand13go.cartera.adaptadores.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import io.github.juand13go.cartera.aplicacion.GenerarRecibo;
import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReciboWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RepositorioPrestamos prestamos;

    @Autowired
    private GenerarRecibo generarRecibo;

    @Autowired
    private Textos textos;

    private long laura() {
        for (Prestamo prestamo : prestamos.listarPorCliente("1000000001")) {
            if (prestamo.getContrato().getFechaDesembolso().equals(LocalDate.of(2026, 8, 10))) {
                return prestamo.getId();
            }
        }
        throw new AssertionError("No está el préstamo de prueba");
    }

    @Test
    void elReciboMuestraElPagoSuDesgloseYLosBotones() throws Exception {
        mvc.perform(get("/prestamos/" + laura() + "/recibos/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Recibo N°")))
                .andExpect(content().string(containsString("Laura Gómez (prueba)")))
                .andExpect(content().string(containsString("10/09/2026 09:15")))
                .andExpect(content().string(containsString("$160.000")))
                .andExpect(content().string(containsString("$2.000.000")))
                .andExpect(content().string(containsString("Imprimir")))
                .andExpect(content().string(containsString("https://wa.me/573000000001?text=")));
    }

    @Test
    void despuesDePagarSeVeElReciboDeEsePago() throws Exception {
        long id = laura();
        mvc.perform(post("/prestamos/" + id + "/pagos").param("monto", "123000"));

        mvc.perform(get("/prestamos/" + id + "/recibos/ultimo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("$123.000")));
    }

    @Test
    void unReciboQueNoExisteVuelveAlDetalleConUnMensaje() throws Exception {
        long id = laura();

        mvc.perform(get("/prestamos/" + id + "/recibos/99"))
                .andExpect(redirectedUrl("/prestamos/" + id))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void elEnlaceDeWhatsappLlevaElTextoDelReciboCodificado() {
        String enlace = textos.enlaceWhatsapp(generarRecibo.ejecutar(laura(), 1));

        assertTrue(enlace.startsWith("https://wa.me/573000000001?text=Recibo%20N%C2%B0%20"));
        assertTrue(enlace.contains("%0A"), "los saltos de línea van como %0A");
        assertFalse(enlace.contains(" "), "no puede tener espacios sin codificar");
        assertFalse(enlace.contains("+"), "los espacios van como %20, no como +");
    }
}
