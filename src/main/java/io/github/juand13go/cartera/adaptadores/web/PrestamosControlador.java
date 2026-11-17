package io.github.juand13go.cartera.adaptadores.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.github.juand13go.cartera.aplicacion.ConsultarPrestamos;
import io.github.juand13go.cartera.aplicacion.CrearPrestamo;
import io.github.juand13go.cartera.aplicacion.CupoExcedidoException;
import io.github.juand13go.cartera.aplicacion.DirectorioClientes;
import io.github.juand13go.cartera.dominio.estructuras.ListaDoble;
import io.github.juand13go.cartera.dominio.modelo.Cliente;
import io.github.juand13go.cartera.dominio.modelo.Prestamo;

/** Pantallas de préstamos: listar los activos y crear un préstamo con su contrato. */
@Controller
public class PrestamosControlador {

    private final DirectorioClientes directorio;
    private final CrearPrestamo crearPrestamo;
    private final ConsultarPrestamos consultarPrestamos;
    private final BigDecimal montoMinimo;

    public PrestamosControlador(DirectorioClientes directorio, CrearPrestamo crearPrestamo,
                                ConsultarPrestamos consultarPrestamos,
                                @Value("${app.monto-minimo}") BigDecimal montoMinimo) {
        this.directorio = directorio;
        this.crearPrestamo = crearPrestamo;
        this.consultarPrestamos = consultarPrestamos;
        this.montoMinimo = montoMinimo;
    }

    /**
     * Lista los préstamos que no se han cerrado, del más reciente al más antiguo (el orden en que los
     * entrega el repositorio). Cada fila busca a su cliente en la TablaHash del directorio: O(1) promedio.
     */
    @GetMapping("/prestamos")
    public String activos(Model modelo) {
        LocalDate hoy = consultarPrestamos.hoy();
        ListaDoble<PrestamoActivo> activos = new ListaDoble<>();
        BigDecimal carteraTotal = BigDecimal.ZERO;
        for (Prestamo prestamo : consultarPrestamos.todos()) {
            if (prestamo.estaCerrado()) {
                continue;
            }
            Cliente cliente = directorio.buscar(prestamo.getCedulaCliente());
            activos.agregarAlFinal(new PrestamoActivo(prestamo.getId(), cliente.getCedula(), cliente.getNombre(),
                    prestamo.getContrato().getFechaDesembolso(), prestamo.getContrato().getCapital(),
                    prestamo.capitalAdeudado(), prestamo.saldoTotal(hoy)));
            carteraTotal = carteraTotal.add(prestamo.capitalAdeudado());
        }
        modelo.addAttribute("activos", activos);
        modelo.addAttribute("carteraTotal", carteraTotal);
        return "prestamos";
    }

    @GetMapping("/prestamos/nuevo")
    public String nuevo(@RequestParam String cedula, Model modelo, RedirectAttributes redireccion) {
        Cliente cliente = directorio.buscar(cedula);
        if (cliente == null) {
            redireccion.addFlashAttribute("error", "No existe un cliente con la cédula " + cedula);
            return "redirect:/clientes";
        }
        return mostrarFormulario(FormularioPrestamo.conValoresPorDefecto(cedula, consultarPrestamos.hoy()), cliente, modelo);
    }

    /**
     * Crea el préstamo. Si supera el cupo y no se ha confirmado, vuelve al formulario con la advertencia
     * y la casilla de confirmación; cualquier otro problema vuelve con su motivo.
     */
    @PostMapping("/prestamos")
    public String crear(@ModelAttribute("formulario") FormularioPrestamo formulario, BindingResult errores,
                        Model modelo, RedirectAttributes redireccion) {
        Cliente cliente = directorio.buscar(formulario.getCedula());
        if (cliente == null) {
            redireccion.addFlashAttribute("error", "No existe un cliente con la cédula " + formulario.getCedula());
            return "redirect:/clientes";
        }
        if (errores.hasErrors()) {
            modelo.addAttribute("error", "Revise los campos: hay números o fechas mal escritos.");
            return mostrarFormulario(formulario, cliente, modelo);
        }
        try {
            Prestamo prestamo = crearPrestamo.ejecutar(cliente.getCedula(), formulario.aContrato(),
                    formulario.isConfirmaSobreCupo());
            redireccion.addFlashAttribute("exito", "Préstamo creado.");
            return "redirect:/prestamos/" + prestamo.getId();
        } catch (CupoExcedidoException e) {
            modelo.addAttribute("cupo", e);
            return mostrarFormulario(formulario, cliente, modelo);
        } catch (IllegalArgumentException e) {
            modelo.addAttribute("error", e.getMessage());
            return mostrarFormulario(formulario, cliente, modelo);
        }
    }

    private String mostrarFormulario(FormularioPrestamo formulario, Cliente cliente, Model modelo) {
        modelo.addAttribute("formulario", formulario);
        modelo.addAttribute("cliente", cliente);
        modelo.addAttribute("montoMinimo", montoMinimo);
        return "prestamo-nuevo";
    }
}
