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

/** Entrar (prestamista o cliente) y salir. */
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

    @PostMapping("/login/prestamista")
    public String entrarPrestamista(@RequestParam(required = false) String usuario,
                                    @RequestParam(required = false) String clave,
                                    HttpServletRequest peticion, Model modelo) {
        try {
            UsuarioSesion prestamista = iniciarSesion.prestamista(usuario, clave);
            SesionWeb.iniciar(peticion, prestamista);
            return "redirect:" + inicioDe(prestamista);
        } catch (IllegalArgumentException e) {
            modelo.addAttribute("errorPrestamista", e.getMessage());
            modelo.addAttribute("usuarioEscrito", usuario);
            return "login";
        }
    }

    @PostMapping("/login/cliente")
    public String entrarCliente(@RequestParam(required = false) String cedula,
                                @RequestParam(required = false) String pin,
                                HttpServletRequest peticion, Model modelo) {
        try {
            UsuarioSesion cliente = iniciarSesion.cliente(cedula, pin);
            SesionWeb.iniciar(peticion, cliente);
            return "redirect:" + inicioDe(cliente);
        } catch (IllegalArgumentException e) {
            modelo.addAttribute("errorCliente", e.getMessage());
            modelo.addAttribute("cedulaEscrita", cedula);
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
