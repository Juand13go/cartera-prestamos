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

    private static final UsuarioSesion LUZ_MARINA = new UsuarioSesion(Rol.CLIENTE, "43586217", "Luz Marina Restrepo Arango");

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

    private long deLuzMarina() {
        return idDe("43586217", LocalDate.of(2026, 8, 18));
    }

    private long deJhonFredy() {
        return idDe("71793468", LocalDate.of(2026, 10, 15));
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
        long id = deLuzMarina();

        mvc.perform(post("/prestamos/" + id + "/pagos").param("monto", "160000"))
                .andExpect(redirectedUrl("/login"));

        assertEquals(2, prestamos.buscarPorId(id).getPagos().tamano());
    }

    @Test
    void elLoginLaHojaDeEstilosYLasFuentesSonPublicos() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/css/estilos.css")).andExpect(status().isOk());
        mvc.perform(get("/fuentes/atkinson-hyperlegible-400.woff2")).andExpect(status().isOk());
    }

    // Cliente

    @Test
    void elClienteQueEntraAlInicioVaASusPrestamos() throws Exception {
        mvc.perform(como(LUZ_MARINA, get("/"))).andExpect(redirectedUrl("/mis-prestamos"));
    }

    @Test
    void elClienteNoPuedeVerLasPantallasDelPrestamista() throws Exception {
        mvc.perform(como(LUZ_MARINA, get("/clientes"))).andExpect(status().isForbidden());
        mvc.perform(como(LUZ_MARINA, get("/clientes/71793468"))).andExpect(status().isForbidden());
        // Ni siquiera la vista de prestamista de su propio préstamo.
        mvc.perform(como(LUZ_MARINA, get("/prestamos/" + deLuzMarina()))).andExpect(status().isForbidden());
    }

    @Test
    void elClienteNoPuedeModificarNadaAunqueEnvieElPostDirectamente() throws Exception {
        long id = deLuzMarina();

        mvc.perform(como(LUZ_MARINA, post("/prestamos/" + id + "/pagos").param("monto", "160000")))
                .andExpect(status().isForbidden());
        mvc.perform(como(LUZ_MARINA, post("/prestamos/" + id + "/deshacer"))).andExpect(status().isForbidden());
        mvc.perform(como(LUZ_MARINA, post("/clientes").param("cedula", "2000000009"))).andExpect(status().isForbidden());

        assertEquals(2, prestamos.buscarPorId(id).getPagos().tamano());
    }

    @Test
    void elClienteNoPuedeVerLosPrestamosDeOtroCliente() throws Exception {
        long jhonFredy = deJhonFredy();

        mvc.perform(como(LUZ_MARINA, get("/mis-prestamos/" + jhonFredy))).andExpect(status().isForbidden());
        mvc.perform(como(LUZ_MARINA, get("/mis-prestamos/" + jhonFredy + "/recibos/1"))).andExpect(status().isForbidden());
    }

    @Test
    void unNumeroDePrestamoQueNoExisteOInvalidoTambienSeProhibe() throws Exception {
        mvc.perform(como(LUZ_MARINA, get("/mis-prestamos/999999"))).andExpect(status().isForbidden());
        mvc.perform(como(LUZ_MARINA, get("/mis-prestamos/abc"))).andExpect(status().isForbidden());
    }

    @Test
    void elClienteSiVeSusPropiosPrestamosYPuedeSalir() throws Exception {
        mvc.perform(como(LUZ_MARINA, get("/mis-prestamos"))).andExpect(status().isOk());
        mvc.perform(como(LUZ_MARINA, get("/mis-prestamos/" + deLuzMarina()))).andExpect(status().isOk());
        mvc.perform(como(LUZ_MARINA, get("/mis-prestamos/" + deLuzMarina() + "/recibos/1"))).andExpect(status().isOk());
        mvc.perform(como(LUZ_MARINA, post("/salir"))).andExpect(redirectedUrl("/login"));
    }

    @Test
    void unaRutaConJsessionidNoSeBloqueaPorError() throws Exception {
        mvc.perform(como(LUZ_MARINA, get("/mis-prestamos;jsessionid=ABC123"))).andExpect(status().isOk());
    }

    // Prestamista

    @Test
    void elPrestamistaVeTodoMenosElPortalDelCliente() throws Exception {
        mvc.perform(como(Sesiones.PRESTAMISTA, get("/"))).andExpect(status().isOk());
        mvc.perform(como(Sesiones.PRESTAMISTA, get("/prestamos/" + deJhonFredy()))).andExpect(status().isOk());
        mvc.perform(como(Sesiones.PRESTAMISTA, get("/mis-prestamos"))).andExpect(status().isForbidden());
    }
}
