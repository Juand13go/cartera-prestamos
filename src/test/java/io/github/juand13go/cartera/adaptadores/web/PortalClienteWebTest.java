package io.github.juand13go.cartera.adaptadores.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import io.github.juand13go.cartera.aplicacion.Rol;
import io.github.juand13go.cartera.aplicacion.UsuarioSesion;
import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/** El portal visto por Laura (cédula 1000000001), que tiene dos préstamos de prueba. */
@SpringBootTest
@AutoConfigureMockMvc
class PortalClienteWebTest {

    private static final UsuarioSesion LAURA = new UsuarioSesion(Rol.CLIENTE, "1000000001", "Laura Gómez (prueba)");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RepositorioPrestamos prestamos;

    private static MockHttpServletRequestBuilder comoLaura(MockHttpServletRequestBuilder peticion) {
        return peticion.sessionAttr(SesionWeb.USUARIO, LAURA);
    }

    private long prestamoMensualDeLaura() {
        for (Prestamo prestamo : prestamos.listarPorCliente("1000000001")) {
            if (prestamo.getContrato().getFechaDesembolso().equals(LocalDate.of(2026, 8, 10))) {
                return prestamo.getId();
            }
        }
        throw new AssertionError("No está el préstamo de prueba");
    }

    @Test
    void laListaMuestraSusDosPrestamosConProgresoYProximoCobro() throws Exception {
        mvc.perform(comoLaura(get("/mis-prestamos")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("por $2.000.000")))
                .andExpect(content().string(containsString("por $300.000")))
                .andExpect(content().string(containsString("<progress")))
                .andExpect(content().string(containsString("Próxima fecha de cobro")))
                .andExpect(content().string(containsString("Próximo pago")))
                .andExpect(content().string(containsString("Para estar al día hoy")))
                .andExpect(content().string(not(containsString("Carlos"))));
    }

    @Test
    void elDetalleEsDeSoloLecturaYTraeElSimulador() throws Exception {
        mvc.perform(comoLaura(get("/mis-prestamos/" + prestamoMensualDeLaura())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Simulador")))
                .andExpect(content().string(containsString("Mis pagos")))
                .andExpect(content().string(not(containsString("Registrar pago"))))
                .andExpect(content().string(not(containsString("Condonar"))))
                .andExpect(content().string(not(containsString("Deshacer"))));
    }

    @Test
    void elSimuladorMuestraElNuevoCapital() throws Exception {
        mvc.perform(comoLaura(get("/mis-prestamos/" + prestamoMensualDeLaura()).param("abono", "2000000")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Capital: antes → después")))
                .andExpect(content().string(containsString("$2.000.000 →")))
                // Base inicial: avisa que abonar no cambia las cuotas.
                .andExpect(content().string(containsString("abonar a capital no cambia el valor de las cuotas")));
    }

    @Test
    void unAbonoQueNoEsNumeroSeAvisa() throws Exception {
        mvc.perform(comoLaura(get("/mis-prestamos/" + prestamoMensualDeLaura()).param("abono", "mucho")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escriba el abono en pesos")));
    }

    @Test
    void veSusRecibosSinElBotonDeWhatsapp() throws Exception {
        long id = prestamoMensualDeLaura();

        mvc.perform(comoLaura(get("/mis-prestamos/" + id + "/recibos/1")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Recibo N°")))
                .andExpect(content().string(containsString("href=\"/mis-prestamos/" + id + "\"")))
                .andExpect(content().string(not(containsString("wa.me"))));
    }
}
