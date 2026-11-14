package io.github.juand13go.cartera.adaptadores.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import io.github.juand13go.cartera.aplicacion.Rol;
import io.github.juand13go.cartera.aplicacion.UsuarioSesion;

/** Ayudas para las pruebas web ahora que todo pasa por el interceptor de roles. */
final class Sesiones {

    static final UsuarioSesion PRESTAMISTA = new UsuarioSesion(Rol.PRESTAMISTA, null, "prestamista");

    private Sesiones() {
    }

    /** Un MockMvc en el que cada petición lleva, por defecto, una sesión de prestamista. */
    static MockMvc comoPrestamista(WebApplicationContext contexto) {
        return MockMvcBuilders.webAppContextSetup(contexto)
                .defaultRequest(get("/").sessionAttr(SesionWeb.USUARIO, PRESTAMISTA))
                .build();
    }
}
