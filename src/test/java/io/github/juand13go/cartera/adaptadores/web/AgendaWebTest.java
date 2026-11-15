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
 * Ese día hay tres cobros: Gloria (quincenal, vence hoy), Luz Marina (mensual, miércoles 18)
 * y Jhon Fredy (semanal, jueves 19).
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

        int gloria = agenda.indexOf("Gloria Patricia Cardona Vélez");
        int luzMarina = agenda.indexOf("Luz Marina Restrepo Arango");
        int jhon = agenda.indexOf("Jhon Fredy Zapata Muñoz");
        assertTrue(gloria >= 0 && luzMarina >= 0 && jhon >= 0, "deben salir los tres cobros");
        assertTrue(gloria < luzMarina && luzMarina < jhon, "van en orden de fecha: lunes, miércoles, jueves");
        // Las cabeceras de calendario: HOY, luego el miércoles, luego el jueves.
        int hoy = agenda.indexOf(">HOY<");
        int miercoles = agenda.indexOf(">MIÉ<");
        int jueves = agenda.indexOf(">JUE<");
        assertTrue(hoy >= 0 && hoy < miercoles && miercoles < jueves);
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
        assertTrue(agenda.contains("?text=Hola%20Gloria%20Patricia%20Cardona%20V%C3%A9lez%2C%20te%20recuerdo%20el%20pago%20de%20"));
        assertTrue(agenda.contains("target=\"_blank\""));
        assertTrue(agenda.contains("aria-label=\"Enviar recordatorio por WhatsApp a Gloria Patricia Cardona Vélez\""));
        assertTrue(agenda.contains("title=\"Enviar recordatorio por WhatsApp a Gloria Patricia Cardona Vélez\""));
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
