package io.github.juand13go.cartera.adaptadores.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import io.github.juand13go.cartera.aplicacion.DirectorioClientes;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClientesControladorTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DirectorioClientes directorio;

    @Test
    void laListaMuestraLosClientesDePrueba() throws Exception {
        mvc.perform(get("/clientes"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Laura Gómez (prueba)")))
                .andExpect(content().string(containsString("Jorge Castaño (prueba)")));
    }

    @Test
    void buscarUnaCedulaQueExisteLlevaASuFicha() throws Exception {
        mvc.perform(get("/clientes").param("cedula", "1000000002"))
                .andExpect(redirectedUrl("/clientes/1000000002"));
    }

    @Test
    void buscarUnaCedulaQueNoExisteAvisa() throws Exception {
        mvc.perform(get("/clientes").param("cedula", "1234567"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No existe un cliente con la cédula 1234567")));
    }

    @Test
    void laFichaMuestraLosDatosLasNotasYLosPrestamos() throws Exception {
        mvc.perform(get("/clientes/1000000002"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Carlos Mejía (prueba)")))
                .andExpect(content().string(containsString("Se atrasa; llamar antes del 5.")))
                .andExpect(content().string(containsString("Nuevo préstamo")))
                .andExpect(content().string(containsString("$1.500.000")));
    }

    @Test
    void laFichaDeUnClienteQueNoExisteVuelveALaLista() throws Exception {
        mvc.perform(get("/clientes/1234567"))
                .andExpect(redirectedUrl("/clientes"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void elFormularioDeClienteNuevoSeMuestra() throws Exception {
        mvc.perform(get("/clientes/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PIN para entrar")));
    }

    @Test
    void crearUnClienteLlevaASuFicha() throws Exception {
        mvc.perform(post("/clientes")
                        .param("cedula", "2000000001")
                        .param("nombre", "Cliente Web (prueba)")
                        .param("telefono", "300 000 0099")
                        .param("cupoMaximo", "1000000")
                        .param("pin", "4321"))
                .andExpect(redirectedUrl("/clientes/2000000001"))
                .andExpect(flash().attribute("exito", "Cliente creado."));

        assertNotNull(directorio.buscar("2000000001"));
    }

    @Test
    void siUnDatoEstaMalVuelveAlFormularioConElMotivoYLoEscrito() throws Exception {
        mvc.perform(post("/clientes")
                        .param("cedula", "2000000002")
                        .param("nombre", "Cliente Web (prueba)")
                        .param("telefono", "123")
                        .param("cupoMaximo", "1000000")
                        .param("pin", "4321"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El teléfono debe tener 10 dígitos")))
                .andExpect(content().string(containsString("Cliente Web (prueba)")));
    }

    @Test
    void unCupoQueNoEsNumeroSeAvisa() throws Exception {
        mvc.perform(post("/clientes")
                        .param("cedula", "2000000003")
                        .param("nombre", "Cliente Web (prueba)")
                        .param("telefono", "3000000099")
                        .param("cupoMaximo", "mucho")
                        .param("pin", "4321"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Revise el cupo")));
    }
}
