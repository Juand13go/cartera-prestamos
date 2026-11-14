package io.github.juand13go.cartera.adaptadores.seguridad;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.juand13go.cartera.aplicacion.IniciarSesion;

/** Con la configuración real: el prestamista entra con la contraseña por defecto, guardada como hash BCrypt. */
@SpringBootTest
class CredencialesPrestamistaTest {

    @Autowired
    private IniciarSesion iniciarSesion;

    @Test
    void elPrestamistaEntraConLaClavePorDefecto() {
        assertTrue(iniciarSesion.entrar("prestamista", "cartera2026").esPrestamista());
    }

    @Test
    void unClienteDePruebaEntraConPin1234() {
        assertTrue(iniciarSesion.entrar("1000000001", "1234").esCliente());
    }
}
