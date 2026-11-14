package io.github.juand13go.cartera.adaptadores.web;

import static org.hamcrest.Matchers.containsString;
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

/** Pide la página como lo haría el navegador y revisa el HTML que devuelve, con los datos de prueba. */
@SpringBootTest
@AutoConfigureMockMvc
class PanelControladorTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    /** Todas las peticiones de esta clase entran como prestamista (el interceptor de roles lo exige). */
    @BeforeEach
    void entrarComoPrestamista() {
        mvc = Sesiones.comoPrestamista(contexto);
    }

    @Test
    void elPanelMuestraLaAgendaYLaMora() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Agenda de cobros")))
                .andExpect(content().string(containsString("Lista de mora")))
                .andExpect(content().string(containsString("Hoy:")));
    }

    @Test
    void carlosApareceEnLaListaDeMora() throws Exception {
        // Carlos dejó de pagar en octubre de 2026: aparece en mora cualquier día después de esa fecha.
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Carlos Mejía (prueba)")));
    }

    @Test
    void laHojaDeEstilosEstaDisponible() throws Exception {
        mvc.perform(get("/css/estilos.css"))
                .andExpect(status().isOk());
    }
}
