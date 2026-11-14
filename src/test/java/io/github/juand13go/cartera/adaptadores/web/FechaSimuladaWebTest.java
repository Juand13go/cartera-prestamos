package io.github.juand13go.cartera.adaptadores.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.web.context.WebApplicationContext;

/** Con app.fecha-simulada, la barra superior avisa que la fecha no es la real. */
@SpringBootTest(properties = "app.fecha-simulada=2027-01-20")
@AutoConfigureMockMvc
class FechaSimuladaWebTest {

    @Autowired
    private WebApplicationContext contexto;

    @Test
    void laBarraMuestraLaFechaSimulada() throws Exception {
        Sesiones.comoPrestamista(contexto).perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hoy (simulado):")))
                .andExpect(content().string(containsString("20/01/2027")));
    }
}
