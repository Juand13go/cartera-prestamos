package io.github.juand13go.cartera.adaptadores.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * La agenda del panel dibujada como tarjetas, con los datos de prueba y la fecha fija en el lunes 16 nov 2026.
 * Ese día hay dos cobros: Marta (quincenal, vence hoy) y Laura (semanal, vence el martes 17).
 */
@SpringBootTest(properties = "app.fecha-simulada=2026-11-16")
@AutoConfigureMockMvc
class AgendaWebTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void entrarComoPrestamista() {
        mvc = Sesiones.comoPrestamista(contexto);
    }

    /** Solo el pedazo de HTML de la agenda: de su título al de la lista de mora, para no confundir nombres de las dos listas. */
    private String agenda() throws Exception {
        String html = mvc.perform(get("/")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return html.substring(html.indexOf("Esta semana"), html.indexOf("Lista de mora"));
    }

    @Test
    void laPrimeraTarjetaEsLaDeFechaMasCercana() throws Exception {
        String agenda = agenda();

        int marta = agenda.indexOf("Marta Ospina (prueba)");
        int laura = agenda.indexOf("Laura Gómez (prueba)");
        assertTrue(marta >= 0 && laura >= 0, "deben salir los dos cobros");
        assertTrue(marta < laura, "el cobro de hoy (Marta) va antes que el de mañana (Laura)");
        // La primera cabecera de calendario es HOY; la segunda, el martes.
        int hoy = agenda.indexOf(">HOY<");
        int martes = agenda.indexOf(">MAR<");
        assertTrue(hoy >= 0 && hoy < martes);
    }

    @Test
    void cadaTarjetaDiceElPrestamoLaCuotaYLaFrecuencia() throws Exception {
        String agenda = agenda();

        assertTrue(agenda.contains(" · quincenal"));
        assertTrue(agenda.contains(" · semanal"));
        assertTrue(agenda.contains("class=\"cobro hoy\""));
    }

    @Test
    void elRecordatorioAbreWhatsappConElTextoCodificado() throws Exception {
        String agenda = agenda();

        assertTrue(agenda.contains("https://wa.me/57"));
        assertTrue(agenda.contains("?text=Hola%20Marta%20Ospina%20%28prueba%29%2C%20te%20recuerdo%20el%20pago%20de%20"));
        assertTrue(agenda.contains("target=\"_blank\""));
        assertTrue(agenda.contains("aria-label=\"Enviar recordatorio por WhatsApp a Marta Ospina (prueba)\""));
        assertTrue(agenda.contains("title=\"Enviar recordatorio por WhatsApp a Marta Ospina (prueba)\""));
    }

    @Test
    void detalleLlevaAlFormularioDePagoConElMontoPrecargado() throws Exception {
        String agenda = agenda();
        assertTrue(agenda.matches("(?s).*href=\"/prestamos/\\d+\\?monto=\\d+#registrar-pago\">Detalle</a>.*"));

        mvc.perform(get("/prestamos/1").param("monto", "160000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"registrar-pago\"")))
                .andExpect(content().string(containsString("value=\"160000\"")));
    }
}
