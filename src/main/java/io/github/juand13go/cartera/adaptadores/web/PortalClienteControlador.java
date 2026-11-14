package io.github.juand13go.cartera.adaptadores.web;

import java.math.BigDecimal;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.juand13go.cartera.aplicacion.ConsultarPrestamos;
import io.github.juand13go.cartera.aplicacion.GenerarRecibo;
import io.github.juand13go.cartera.aplicacion.SimularAbono;
import io.github.juand13go.cartera.aplicacion.UsuarioSesion;

/**
 * Portal del cliente: solo lectura y solo GET. Que el cliente vea únicamente SUS préstamos
 * lo garantiza el interceptor de roles, antes de que la petición llegue aquí.
 */
@Controller
public class PortalClienteControlador {

    private final ConsultarPrestamos consultar;
    private final SimularAbono simularAbono;
    private final GenerarRecibo generarRecibo;

    public PortalClienteControlador(ConsultarPrestamos consultar, SimularAbono simularAbono,
                                    GenerarRecibo generarRecibo) {
        this.consultar = consultar;
        this.simularAbono = simularAbono;
        this.generarRecibo = generarRecibo;
    }

    /** Sus préstamos con barra de progreso, próxima fecha de cobro, valor para estar al día y para cancelar. */
    @GetMapping("/mis-prestamos")
    public String misPrestamos(HttpServletRequest peticion, Model modelo) {
        UsuarioSesion usuario = SesionWeb.usuario(peticion);
        modelo.addAttribute("prestamos", consultar.deCliente(usuario.cedula()));
        return "mis-prestamos";
    }

    /** Detalle de solo lectura. Si llega ?abono=, muestra también el resultado del simulador. */
    @GetMapping("/mis-prestamos/{id}")
    public String miPrestamo(@PathVariable long id, @RequestParam(required = false) String abono,
                             Model modelo, RedirectAttributes redireccion) {
        try {
            modelo.addAttribute("prestamo", consultar.porId(id));
        } catch (IllegalArgumentException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/mis-prestamos";
        }
        if (abono != null && !abono.isBlank()) {
            modelo.addAttribute("abonoEscrito", abono);
            try {
                modelo.addAttribute("simulacion", simularAbono.ejecutar(id, new BigDecimal(abono.trim())));
            } catch (NumberFormatException e) {
                modelo.addAttribute("errorSimulacion", "Escriba el abono en pesos, por ejemplo 500000");
            } catch (IllegalArgumentException | IllegalStateException e) {
                modelo.addAttribute("errorSimulacion", e.getMessage());
            }
        }
        return "mi-prestamo";
    }

    /** Sus recibos, con la misma plantilla del prestamista pero sin el botón de WhatsApp. */
    @GetMapping("/mis-prestamos/{id}/recibos/{numero}")
    public String recibo(@PathVariable long id, @PathVariable int numero, Model modelo, RedirectAttributes redireccion) {
        try {
            modelo.addAttribute("recibo", generarRecibo.ejecutar(id, numero));
        } catch (IllegalArgumentException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/mis-prestamos/" + id;
        }
        modelo.addAttribute("volverA", "/mis-prestamos/" + id);
        return "recibo";
    }
}
