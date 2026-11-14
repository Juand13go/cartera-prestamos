package io.github.juand13go.cartera.adaptadores.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.juand13go.cartera.aplicacion.IniciarSesion;
import io.github.juand13go.cartera.aplicacion.UsuarioSesion;

/** Entrar (un solo formulario para prestamista y clientes) y salir. */
@Controller
public class LoginControlador {

    private final IniciarSesion iniciarSesion;

    public LoginControlador(IniciarSesion iniciarSesion) {
        this.iniciarSesion = iniciarSesion;
    }

    /** Si ya hay sesión, no tiene sentido volver a entrar: va a su pantalla de inicio. */
    @GetMapping("/login")
    public String login(HttpServletRequest peticion) {
        UsuarioSesion usuario = SesionWeb.usuario(peticion);
        if (usuario != null) {
            return "redirect:" + inicioDe(usuario);
        }
        return "login";
    }

    /**
     * Un solo formulario para todos: el usuario no escoge su rol, IniciarSesion lo deduce de la cuenta.
     * Si sale bien, va al inicio de su rol; si no, vuelve al login con el mensaje y lo que escribió.
     */
    @PostMapping("/login")
    public String entrar(@RequestParam(required = false) String identificador,
                         @RequestParam(required = false) String clave,
                         HttpServletRequest peticion, Model modelo) {
        try {
            UsuarioSesion usuario = iniciarSesion.entrar(identificador, clave);
            SesionWeb.iniciar(peticion, usuario);
            return "redirect:" + inicioDe(usuario);
        } catch (IllegalArgumentException e) {
            modelo.addAttribute("error", e.getMessage());
            modelo.addAttribute("identificadorEscrito", identificador);
            return "login";
        }
    }

    @PostMapping("/salir")
    public String salir(HttpServletRequest peticion, RedirectAttributes redireccion) {
        SesionWeb.cerrar(peticion);
        redireccion.addFlashAttribute("exito", "Sesión cerrada.");
        return "redirect:/login";
    }

    /** Pantalla de inicio según el rol. */
    static String inicioDe(UsuarioSesion usuario) {
        return usuario.esPrestamista() ? "/" : "/mis-prestamos";
    }
}
