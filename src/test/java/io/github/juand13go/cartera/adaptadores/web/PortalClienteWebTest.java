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

/** El portal visto por Luz Marina (cédula 43586217), que tiene un préstamo mensual de prueba. */
@SpringBootTest
@AutoConfigureMockMvc
class PortalClienteWebTest {

    private static final UsuarioSesion LUZ_MARINA = new UsuarioSesion(Rol.CLIENTE, "43586217", "Luz Marina Restrepo Arango");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RepositorioPrestamos prestamos;

    private static MockHttpServletRequestBuilder comoLuzMarina(MockHttpServletRequestBuilder peticion) {
        return peticion.sessionAttr(SesionWeb.USUARIO, LUZ_MARINA);
    }

    private long prestamoDeLuzMarina() {
        for (Prestamo prestamo : prestamos.listarPorCliente("43586217")) {
            if (prestamo.getContrato().getFechaDesembolso().equals(LocalDate.of(2026, 8, 18))) {
                return prestamo.getId();
            }
        }
        throw new AssertionError("No está el préstamo de prueba");
    }

    @Test
    void laListaMuestraSuPrestamoConProgresoYProximoCobro() throws Exception {
        mvc.perform(comoLuzMarina(get("/mis-prestamos")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("por $2.000.000")))
                .andExpect(content().string(containsString("<progress")))
                .andExpect(content().string(containsString("Próxima fecha de cobro")))
                .andExpect(content().string(containsString("Próximo pago")))
                .andExpect(content().string(containsString("Para estar al día hoy")))
                .andExpect(content().string(not(containsString("Jhon Fredy"))));
    }

    @Test
    void elDetalleEsDeSoloLecturaYTraeElSimulador() throws Exception {
        mvc.perform(comoLuzMarina(get("/mis-prestamos/" + prestamoDeLuzMarina())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Simulador")))
                .andExpect(content().string(containsString("Mis pagos")))
                .andExpect(content().string(not(containsString("Registrar pago"))))
                .andExpect(content().string(not(containsString("Condonar"))))
                .andExpect(content().string(not(containsString("Deshacer"))));
    }

    @Test
    void elSimuladorMuestraElNuevoCapital() throws Exception {
        mvc.perform(comoLuzMarina(get("/mis-prestamos/" + prestamoDeLuzMarina()).param("abono", "2000000")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Capital: antes → después")))
                .andExpect(content().string(containsString("$2.000.000 →")))
                // Base inicial: avisa que abonar no cambia las cuotas.
                .andExpect(content().string(containsString("abonar a capital no cambia el valor de las cuotas")));
    }

    @Test
    void unAbonoQueNoEsNumeroSeAvisa() throws Exception {
        mvc.perform(comoLuzMarina(get("/mis-prestamos/" + prestamoDeLuzMarina()).param("abono", "mucho")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escriba el abono en pesos")));
    }

    @Test
    void veSusRecibosSinElBotonDeWhatsapp() throws Exception {
        long id = prestamoDeLuzMarina();

        mvc.perform(comoLuzMarina(get("/mis-prestamos/" + id + "/recibos/1")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Recibo N°")))
                .andExpect(content().string(containsString("href=\"/mis-prestamos/" + id + "\"")))
                .andExpect(content().string(not(containsString("wa.me"))));
    }
}
