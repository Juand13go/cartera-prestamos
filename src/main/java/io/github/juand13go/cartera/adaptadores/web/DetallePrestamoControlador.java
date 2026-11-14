package io.github.juand13go.cartera.adaptadores.web;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.juand13go.cartera.aplicacion.CancelarPrestamo;
import io.github.juand13go.cartera.aplicacion.CondonarMulta;
import io.github.juand13go.cartera.aplicacion.ConsultarPrestamos;
import io.github.juand13go.cartera.aplicacion.DeshacerUltimoPago;
import io.github.juand13go.cartera.aplicacion.DirectorioClientes;
import io.github.juand13go.cartera.aplicacion.RegistrarPago;
import io.github.juand13go.cartera.dominio.modelo.Pago;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/**
 * Detalle de un préstamo y sus cuatro acciones. Cada acción es un POST que, salga bien o mal,
 * redirige: al recibo si hubo pago, o de vuelta al detalle con un mensaje.
 */
@Controller
public class DetallePrestamoControlador {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ConsultarPrestamos consultar;
    private final DirectorioClientes directorio;
    private final RegistrarPago registrarPago;
    private final DeshacerUltimoPago deshacerUltimoPago;
    private final CondonarMulta condonarMulta;
    private final CancelarPrestamo cancelarPrestamo;

    public DetallePrestamoControlador(ConsultarPrestamos consultar, DirectorioClientes directorio,
                                      RegistrarPago registrarPago, DeshacerUltimoPago deshacerUltimoPago,
                                      CondonarMulta condonarMulta, CancelarPrestamo cancelarPrestamo) {
        this.consultar = consultar;
        this.directorio = directorio;
        this.registrarPago = registrarPago;
        this.deshacerUltimoPago = deshacerUltimoPago;
        this.condonarMulta = condonarMulta;
        this.cancelarPrestamo = cancelarPrestamo;
    }

    @GetMapping("/prestamos/{id}")
    public String detalle(@PathVariable long id, Model modelo, RedirectAttributes redireccion) {
        Prestamo prestamo;
        try {
            prestamo = consultar.porId(id);
        } catch (IllegalArgumentException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
        modelo.addAttribute("prestamo", prestamo);
        modelo.addAttribute("cliente", directorio.buscar(prestamo.getCedulaCliente()));
        modelo.addAttribute("valorParaCancelar", prestamo.valorParaCancelar(consultar.hoy()));
        return "prestamo";
    }

    @PostMapping("/prestamos/{id}/pagos")
    public String pagar(@PathVariable long id, @RequestParam(required = false) String monto,
                        RedirectAttributes redireccion) {
        try {
            Pago pago = registrarPago.ejecutar(id, leerMonto(monto));
            redireccion.addFlashAttribute("exito", "Pago de " + Textos.pesos(pago.getMonto()) + " registrado.");
            return "redirect:/prestamos/" + id + "/recibos/ultimo";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/prestamos/" + id;
        }
    }

    @PostMapping("/prestamos/{id}/deshacer")
    public String deshacer(@PathVariable long id, RedirectAttributes redireccion) {
        try {
            Pago deshecho = deshacerUltimoPago.ejecutar(id);
            redireccion.addFlashAttribute("exito", "Se deshizo el pago de " + Textos.pesos(deshecho.getMonto())
                    + " del " + deshecho.getFecha().format(FECHA) + ".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/prestamos/" + id;
    }

    @PostMapping("/prestamos/{id}/cuotas/{numero}/condonar")
    public String condonar(@PathVariable long id, @PathVariable int numero, RedirectAttributes redireccion) {
        try {
            condonarMulta.ejecutar(id, numero);
            redireccion.addFlashAttribute("exito", "Se condonó la multa de la cuota " + numero + ".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/prestamos/" + id;
    }

    @PostMapping("/prestamos/{id}/cancelar")
    public String cancelar(@PathVariable long id, RedirectAttributes redireccion) {
        try {
            Pago pago = cancelarPrestamo.ejecutar(id);
            redireccion.addFlashAttribute("exito", "Préstamo cancelado con " + Textos.pesos(pago.getMonto()) + ".");
            return "redirect:/prestamos/" + id + "/recibos/ultimo";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/prestamos/" + id;
        }
    }

    /** El monto llega como texto; si no es un número, se avisa con un mensaje en vez de un error de la página. */
    private static BigDecimal leerMonto(String monto) {
        try {
            return new BigDecimal(monto.trim());
        } catch (NullPointerException | NumberFormatException e) {
            throw new IllegalArgumentException("Escriba el monto en pesos, por ejemplo 160000");
        }
    }
}
