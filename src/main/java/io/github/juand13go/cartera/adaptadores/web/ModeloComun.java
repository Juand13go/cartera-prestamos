package io.github.juand13go.cartera.adaptadores.web;

import java.time.Clock;
import java.time.LocalDate;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import io.github.juand13go.cartera.aplicacion.UsuarioSesion;

/**
 * Datos que necesitan todas las vistas: la fecha del reloj, si esa fecha es simulada (para avisarlo en la barra)
 * y quién inició sesión (para el menú).
 */
@ControllerAdvice
public class ModeloComun {

    private final Clock reloj;
    private final boolean fechaSimulada;

    public ModeloComun(Clock reloj, @Value("${app.fecha-simulada:}") String fechaSimulada) {
        this.reloj = reloj;
        this.fechaSimulada = fechaSimulada != null && !fechaSimulada.isBlank();
    }

    @ModelAttribute("hoy")
    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /** true si app.fecha-simulada tiene un valor: la barra muestra "Hoy (simulado)" para que nadie se confunda en la demo. */
    @ModelAttribute("fechaSimulada")
    public boolean fechaSimulada() {
        return fechaSimulada;
    }

    /** Quién inició sesión, o null en la pantalla de login. Thymeleaf 3.1 ya no deja leer la sesión desde la vista. */
    @ModelAttribute("usuario")
    public UsuarioSesion usuario(HttpServletRequest peticion) {
        return SesionWeb.usuario(peticion);
    }
}
