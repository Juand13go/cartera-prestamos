package io.github.juand13go.cartera.adaptadores.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/** Pruebas del detalle y sus acciones con los datos de prueba. Cada prueba se deshace al terminar. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DetallePrestamoWebTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    /** Todas las peticiones de esta clase entran como prestamista (el interceptor de roles lo exige). */
    @BeforeEach
    void entrarComoPrestamista() {
        mvc = Sesiones.comoPrestamista(contexto);
    }

    @Autowired
    private RepositorioPrestamos prestamos;

    /** Id del préstamo de prueba de ese cliente con esa fecha de desembolso (no se dan por sabidos los ids). */
    private long idDe(String cedula, LocalDate desembolso) {
        for (Prestamo prestamo : prestamos.listarPorCliente(cedula)) {
            if (prestamo.getContrato().getFechaDesembolso().equals(desembolso)) {
                return prestamo.getId();
            }
        }
        throw new AssertionError("No está el préstamo de prueba");
    }

    private long laura() {
        return idDe("1000000001", LocalDate.of(2026, 8, 10));
    }

    private long carlos() {
        return idDe("1000000002", LocalDate.of(2026, 7, 5));
    }

    @Test
    void elDetalleMuestraContratoCronogramaPagosYAcciones() throws Exception {
        mvc.perform(get("/prestamos/" + carlos()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Carlos Mejía (prueba)")))
                .andExpect(content().string(containsString("$2.000 por día de atraso, 2 días de gracia")))
                .andExpect(content().string(containsString("Mensual, el día 5")))
                .andExpect(content().string(containsString("En mora")))
                .andExpect(content().string(containsString("Condonar")))
                .andExpect(content().string(containsString("Registrar pago")))
                .andExpect(content().string(containsString("Deshacer el último pago")));
    }

    @Test
    void unPrestamoQueNoExisteVuelveAlPanel() throws Exception {
        mvc.perform(get("/prestamos/999999"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "No existe el préstamo 999999"));
    }

    @Test
    void registrarUnPagoLlevaAlReciboYQuedaGuardado() throws Exception {
        long id = laura();

        mvc.perform(post("/prestamos/" + id + "/pagos").param("monto", "160000"))
                .andExpect(redirectedUrl("/prestamos/" + id + "/recibos/ultimo"))
                .andExpect(flash().attribute("exito", "Pago de $160.000 registrado."));

        assertEquals(4, prestamos.buscarPorId(id).getPagos().tamano());
    }

    @Test
    void unMontoQueNoEsNumeroVuelveAlDetalleConUnMensaje() throws Exception {
        long id = laura();

        mvc.perform(post("/prestamos/" + id + "/pagos").param("monto", "mucho"))
                .andExpect(redirectedUrl("/prestamos/" + id))
                .andExpect(flash().attribute("error", "Escriba el monto en pesos, por ejemplo 160000"));
    }

    @Test
    void deshacerQuitaElUltimoPago() throws Exception {
        long id = laura();

        mvc.perform(post("/prestamos/" + id + "/deshacer"))
                .andExpect(redirectedUrl("/prestamos/" + id))
                .andExpect(flash().attribute("exito", "Se deshizo el pago de $160.000 del 10/11/2026."));

        assertEquals(2, prestamos.buscarPorId(id).getPagos().tamano());
    }

    @Test
    void condonarLaMultaDeUnaCuotaQuedaGuardado() throws Exception {
        long id = carlos();

        mvc.perform(post("/prestamos/" + id + "/cuotas/3/condonar"))
                .andExpect(redirectedUrl("/prestamos/" + id))
                .andExpect(flash().attribute("exito", "Se condonó la multa de la cuota 3."));

        assertTrue(prestamos.buscarPorId(id).getCronograma().obtener(2).estaMultaCondonada());
    }

    @Test
    void cancelarCierraElPrestamoYLlevaAlRecibo() throws Exception {
        long marta = idDe("1000000003", LocalDate.of(2026, 9, 15));

        mvc.perform(post("/prestamos/" + marta + "/cancelar"))
                .andExpect(redirectedUrl("/prestamos/" + marta + "/recibos/ultimo"));

        assertTrue(prestamos.buscarPorId(marta).estaCerrado());
    }

    @Test
    void unPrestamoCerradoNoMuestraAccionesYNoSePuedeCancelar() throws Exception {
        long jorge = idDe("1000000004", LocalDate.of(2026, 6, 20));

        mvc.perform(get("/prestamos/" + jorge))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cancelación")));
        mvc.perform(post("/prestamos/" + jorge + "/cancelar"))
                .andExpect(redirectedUrl("/prestamos/" + jorge))
                .andExpect(flash().attribute("error", "El préstamo ya está cerrado"));
    }
}
