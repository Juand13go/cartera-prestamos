package io.github.juand13go.cartera.adaptadores.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.juand13go.cartera.aplicacion.GenerarRecibo;
import io.github.juand13go.cartera.dominio.modelo.Recibo;

/** Recibo imprimible de un pago, con el enlace para enviarlo por WhatsApp. */
@Controller
public class ReciboControlador {

    private final GenerarRecibo generarRecibo;
    private final Textos textos;

    public ReciboControlador(GenerarRecibo generarRecibo, Textos textos) {
        this.generarRecibo = generarRecibo;
        this.textos = textos;
    }

    /** Recibo del pago número {@code numero} del préstamo (el primero es el 1). */
    @GetMapping("/prestamos/{id}/recibos/{numero}")
    public String recibo(@PathVariable long id, @PathVariable int numero, Model modelo, RedirectAttributes redireccion) {
        try {
            return mostrar(generarRecibo.ejecutar(id, numero), modelo);
        } catch (IllegalArgumentException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/prestamos/" + id;
        }
    }

    /** Recibo del pago más reciente: aquí se llega después de pagar o cancelar. */
    @GetMapping("/prestamos/{id}/recibos/ultimo")
    public String ultimo(@PathVariable long id, Model modelo, RedirectAttributes redireccion) {
        try {
            return mostrar(generarRecibo.ultimo(id), modelo);
        } catch (IllegalArgumentException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/prestamos/" + id;
        }
    }

    /**
     * El enlace de WhatsApp se arma aquí y no en la plantilla: por seguridad, Thymeleaf no deja
     * llamar beans dentro de th:href (evita que un enlace pueda ejecutar código).
     */
    private String mostrar(Recibo recibo, Model modelo) {
        modelo.addAttribute("recibo", recibo);
        modelo.addAttribute("enlaceWhatsapp", textos.enlaceWhatsapp(recibo));
        return "recibo";
    }
}
