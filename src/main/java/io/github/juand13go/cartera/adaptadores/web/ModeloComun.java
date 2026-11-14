package io.github.juand13go.cartera.adaptadores.web;

import java.time.Clock;
import java.time.LocalDate;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import io.github.juand13go.cartera.aplicacion.UsuarioSesion;

/** Datos que necesitan todas las vistas: la fecha del reloj (real o simulada) y quién inició sesión (para el menú). */
@ControllerAdvice
public class ModeloComun {

    private final Clock reloj;

    public ModeloComun(Clock reloj) {
        this.reloj = reloj;
    }

    @ModelAttribute("hoy")
    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /** Quién inició sesión, o null en la pantalla de login. Thymeleaf 3.1 ya no deja leer la sesión desde la vista. */
    @ModelAttribute("usuario")
    public UsuarioSesion usuario(HttpServletRequest peticion) {
        return SesionWeb.usuario(peticion);
    }
}
