package io.github.juand13go.cartera.adaptadores.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import io.github.juand13go.cartera.aplicacion.Rol;
import io.github.juand13go.cartera.aplicacion.UsuarioSesion;
import io.github.juand13go.cartera.aplicacion.puertos.RepositorioPrestamos;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/**
 * El interceptor bloquea en el backend: estas pruebas mandan las peticiones directamente,
 * como lo haría alguien que escribe la dirección a mano o usa otra herramienta, sin pasar por los botones.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InterceptorRolesTest {

    private static final UsuarioSesion LAURA = new UsuarioSesion(Rol.CLIENTE, "1000000001", "Laura Gómez (prueba)");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RepositorioPrestamos prestamos;

    private static MockHttpServletRequestBuilder como(UsuarioSesion usuario, MockHttpServletRequestBuilder peticion) {
        return peticion.sessionAttr(SesionWeb.USUARIO, usuario);
    }

    private long idDe(String cedula, LocalDate desembolso) {
        for (Prestamo prestamo : prestamos.listarPorCliente(cedula)) {
            if (prestamo.getContrato().getFechaDesembolso().equals(desembolso)) {
                return prestamo.getId();
            }
        }
        throw new AssertionError("No está el préstamo de prueba");
    }

    private long deLaura() {
        return idDe("1000000001", LocalDate.of(2026, 8, 10));
    }

    private long deCarlos() {
        return idDe("1000000002", LocalDate.of(2026, 7, 5));
    }

    // Sin sesión

    @Test
    void sinSesionTodoLlevaAlLogin() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/clientes")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/mis-prestamos")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void sinSesionUnPostNoSeEjecuta() throws Exception {
        long id = deLaura();

        mvc.perform(post("/prestamos/" + id + "/pagos").param("monto", "160000"))
                .andExpect(redirectedUrl("/login"));

        assertEquals(3, prestamos.buscarPorId(id).getPagos().tamano());
    }

    @Test
    void elLoginYLaHojaDeEstilosSonPublicos() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/css/estilos.css")).andExpect(status().isOk());
    }

    // Cliente

    @Test
    void elClienteQueEntraAlInicioVaASusPrestamos() throws Exception {
        mvc.perform(como(LAURA, get("/"))).andExpect(redirectedUrl("/mis-prestamos"));
    }

    @Test
    void elClienteNoPuedeVerLasPantallasDelPrestamista() throws Exception {
        mvc.perform(como(LAURA, get("/clientes"))).andExpect(status().isForbidden());
        mvc.perform(como(LAURA, get("/clientes/1000000002"))).andExpect(status().isForbidden());
        // Ni siquiera la vista de prestamista de su propio préstamo.
        mvc.perform(como(LAURA, get("/prestamos/" + deLaura()))).andExpect(status().isForbidden());
    }

    @Test
    void elClienteNoPuedeModificarNadaAunqueEnvieElPostDirectamente() throws Exception {
        long id = deLaura();

        mvc.perform(como(LAURA, post("/prestamos/" + id + "/pagos").param("monto", "160000")))
                .andExpect(status().isForbidden());
        mvc.perform(como(LAURA, post("/prestamos/" + id + "/deshacer"))).andExpect(status().isForbidden());
        mvc.perform(como(LAURA, post("/clientes").param("cedula", "2000000009"))).andExpect(status().isForbidden());

        assertEquals(3, prestamos.buscarPorId(id).getPagos().tamano());
    }

    @Test
    void elClienteNoPuedeVerLosPrestamosDeOtroCliente() throws Exception {
        long carlos = deCarlos();

        mvc.perform(como(LAURA, get("/mis-prestamos/" + carlos))).andExpect(status().isForbidden());
        mvc.perform(como(LAURA, get("/mis-prestamos/" + carlos + "/recibos/1"))).andExpect(status().isForbidden());
    }

    @Test
    void unNumeroDePrestamoQueNoExisteOInvalidoTambienSeProhibe() throws Exception {
        mvc.perform(como(LAURA, get("/mis-prestamos/999999"))).andExpect(status().isForbidden());
        mvc.perform(como(LAURA, get("/mis-prestamos/abc"))).andExpect(status().isForbidden());
    }

    @Test
    void elClienteSiVeSusPropiosPrestamosYPuedeSalir() throws Exception {
        mvc.perform(como(LAURA, get("/mis-prestamos"))).andExpect(status().isOk());
        mvc.perform(como(LAURA, get("/mis-prestamos/" + deLaura()))).andExpect(status().isOk());
        mvc.perform(como(LAURA, get("/mis-prestamos/" + deLaura() + "/recibos/1"))).andExpect(status().isOk());
        mvc.perform(como(LAURA, post("/salir"))).andExpect(redirectedUrl("/login"));
    }

    @Test
    void unaRutaConJsessionidNoSeBloqueaPorError() throws Exception {
        mvc.perform(como(LAURA, get("/mis-prestamos;jsessionid=ABC123"))).andExpect(status().isOk());
    }

    // Prestamista

    @Test
    void elPrestamistaVeTodoMenosElPortalDelCliente() throws Exception {
        mvc.perform(como(Sesiones.PRESTAMISTA, get("/"))).andExpect(status().isOk());
        mvc.perform(como(Sesiones.PRESTAMISTA, get("/prestamos/" + deCarlos()))).andExpect(status().isOk());
        mvc.perform(como(Sesiones.PRESTAMISTA, get("/mis-prestamos"))).andExpect(status().isForbidden());
    }
}
