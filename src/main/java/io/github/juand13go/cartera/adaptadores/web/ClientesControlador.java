package io.github.juand13go.cartera.adaptadores.web;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.juand13go.cartera.aplicacion.ConsultarPrestamos;
import io.github.juand13go.cartera.aplicacion.CrearCliente;
import io.github.juand13go.cartera.aplicacion.DirectorioClientes;
import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.modelo.Cliente;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/** Pantallas de clientes: listar, buscar por cédula, crear y ver la ficha con sus préstamos. */
@Controller
public class ClientesControlador {

    private final DirectorioClientes directorio;
    private final CrearCliente crearCliente;
    private final ConsultarPrestamos consultarPrestamos;

    public ClientesControlador(DirectorioClientes directorio, CrearCliente crearCliente,
                               ConsultarPrestamos consultarPrestamos) {
        this.directorio = directorio;
        this.crearCliente = crearCliente;
        this.consultarPrestamos = consultarPrestamos;
    }

    /** Lista de clientes. Si llega una cédula, la busca en la tabla hash y, si existe, va directo a su ficha. */
    @GetMapping("/clientes")
    public String listar(@RequestParam(required = false) String cedula, Model modelo) {
        if (cedula != null && !cedula.isBlank()) {
            Cliente encontrado = directorio.buscar(cedula.replace(" ", ""));
            if (encontrado != null) {
                return "redirect:/clientes/" + encontrado.getCedula();
            }
            modelo.addAttribute("error", "No existe un cliente con la cédula " + cedula);
        }
        modelo.addAttribute("clientes", directorio.listarPorNombre());
        modelo.addAttribute("cedulaBuscada", cedula);
        return "clientes";
    }

    @GetMapping("/clientes/nuevo")
    public String nuevo(Model modelo) {
        modelo.addAttribute("formulario", new FormularioCliente());
        return "cliente-nuevo";
    }

    /** Si algo falla, vuelve a mostrar el formulario con lo que se escribió y el motivo del error. */
    @PostMapping("/clientes")
    public String crear(@ModelAttribute("formulario") FormularioCliente formulario, BindingResult errores,
                        Model modelo, RedirectAttributes redireccion) {
        if (errores.hasErrors()) {
            modelo.addAttribute("error", "Revise el cupo: debe ser un número en pesos.");
            return "cliente-nuevo";
        }
        try {
            Cliente cliente = crearCliente.ejecutar(formulario.aDatos());
            redireccion.addFlashAttribute("exito", "Cliente creado.");
            return "redirect:/clientes/" + cliente.getCedula();
        } catch (IllegalArgumentException e) {
            modelo.addAttribute("error", e.getMessage());
            return "cliente-nuevo";
        }
    }

    /** Ficha del cliente: sus datos, notas internas, deuda frente al cupo y sus préstamos. */
    @GetMapping("/clientes/{cedula}")
    public String ficha(@PathVariable String cedula, Model modelo, RedirectAttributes redireccion) {
        Cliente cliente = directorio.buscar(cedula);
        if (cliente == null) {
            redireccion.addFlashAttribute("error", "No existe un cliente con la cédula " + cedula);
            return "redirect:/clientes";
        }
        ListaDoble<Prestamo> prestamos = consultarPrestamos.deCliente(cedula);
        modelo.addAttribute("cliente", cliente);
        modelo.addAttribute("prestamos", prestamos);
        modelo.addAttribute("deudaVigente", deudaVigente(prestamos));
        return "cliente";
    }

    /** Capital que debe en sus préstamos activos: la misma cuenta que usa el cupo al crear un préstamo. */
    private static BigDecimal deudaVigente(ListaDoble<Prestamo> prestamos) {
        BigDecimal total = BigDecimal.ZERO;
        for (Prestamo prestamo : prestamos) {
            if (!prestamo.estaCerrado()) {
                total = total.add(prestamo.capitalAdeudado());
            }
        }
        return total;
    }
}
