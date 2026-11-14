package io.github.juand13go.cartera.adaptadores.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import io.github.juand13go.cartera.aplicacion.Rol;
import io.github.juand13go.cartera.aplicacion.UsuarioSesion;

@SpringBootTest
@AutoConfigureMockMvc
class LoginWebTest {

    @Autowired
    private MockMvc mvc;

    private static UsuarioSesion usuarioDe(MvcResult resultado) {
        return (UsuarioSesion) resultado.getRequest().getSession().getAttribute(SesionWeb.USUARIO);
    }

    @Test
    void laPantallaDeLoginTieneUnSoloFormularioSinEscogerRol() throws Exception {
        String pagina = mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cédula o usuario")))
                .andExpect(content().string(containsString("Contraseña")))
                .andExpect(content().string(containsString("action=\"/login\"")))
                .andReturn().getResponse().getContentAsString();

        assertFalse(pagina.contains("Soy cliente"));
        assertFalse(pagina.contains("maxlength"));
    }

    @Test
    void elPrestamistaEntraYVaAlPanel() throws Exception {
        MvcResult resultado = mvc.perform(post("/login").param("identificador", "prestamista").param("clave", "cartera2026"))
                .andExpect(redirectedUrl("/"))
                .andReturn();

        assertEquals(Rol.PRESTAMISTA, usuarioDe(resultado).rol());
    }

    @Test
    void conUnaClaveEquivocadaVuelveAlLoginConElMensaje() throws Exception {
        mvc.perform(post("/login").param("identificador", "prestamista").param("clave", "otra"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuario o clave incorrectos")));
    }

    @Test
    void elClienteEntraConCedulaYPinYVaASusPrestamos() throws Exception {
        MvcResult resultado = mvc.perform(post("/login").param("identificador", "1000000001").param("clave", "1234"))
                .andExpect(redirectedUrl("/mis-prestamos"))
                .andReturn();

        UsuarioSesion usuario = usuarioDe(resultado);
        assertEquals(Rol.CLIENTE, usuario.rol());
        assertEquals("1000000001", usuario.cedula());
    }

    @Test
    void conUnPinEquivocadoVuelveAlLoginSinPerderLaCedula() throws Exception {
        mvc.perform(post("/login").param("identificador", "1000000001").param("clave", "9999"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuario o clave incorrectos")))
                .andExpect(content().string(containsString("value=\"1000000001\"")));
    }

    @Test
    void alEntrarSeDescartaLaSesionAnterior() throws Exception {
        MockHttpSession anterior = new MockHttpSession();

        mvc.perform(post("/login").session(anterior).param("identificador", "1000000001").param("clave", "1234"));

        assertTrue(anterior.isInvalid());
    }

    @Test
    void salirCierraLaSesion() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(SesionWeb.USUARIO, new UsuarioSesion(Rol.PRESTAMISTA, null, "prestamista"));

        mvc.perform(post("/salir").session(sesion))
                .andExpect(redirectedUrl("/login"));

        assertTrue(sesion.isInvalid());
    }

    @Test
    void conSesionElMenuMuestraElNombreYElBotonSalir() throws Exception {
        mvc.perform(get("/").sessionAttr(SesionWeb.USUARIO, new UsuarioSesion(Rol.PRESTAMISTA, null, "prestamista")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Salir")))
                .andExpect(content().string(containsString("Clientes")));
    }

    @Test
    void siYaHaySesionElLoginLlevaAlInicio() throws Exception {
        mvc.perform(get("/login").sessionAttr(SesionWeb.USUARIO, new UsuarioSesion(Rol.CLIENTE, "1000000001", "Laura")))
                .andExpect(redirectedUrl("/mis-prestamos"));
    }

    @Test
    void sinSesionElMenuNoMuestraOpcionesNiBotonSalir() throws Exception {
        String pagina = mvc.perform(get("/login")).andReturn().getResponse().getContentAsString();

        assertFalse(pagina.contains("Mis préstamos"));
        assertFalse(pagina.contains(">Salir<"));
    }

    @Test
    void lasRutasViejasYaNoInicianSesion() throws Exception {
        MvcResult resultado = mvc.perform(post("/login/cliente").param("cedula", "1000000001").param("pin", "1234"))
                .andExpect(redirectedUrl("/login"))
                .andReturn();

        // El interceptor la manda al login sin crear ninguna sesión: nadie entró.
        assertNull(resultado.getRequest().getSession(false));
    }
}
