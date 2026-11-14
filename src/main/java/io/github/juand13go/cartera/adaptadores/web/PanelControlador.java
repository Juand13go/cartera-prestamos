package io.github.juand13go.cartera.adaptadores.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.juand13go.cartera.aplicacion.ArmarPanel;

/** Pantalla de inicio del prestamista: agenda de cobros y lista de mora. */
@Controller
public class PanelControlador {

    private final ArmarPanel armarPanel;

    public PanelControlador(ArmarPanel armarPanel) {
        this.armarPanel = armarPanel;
    }

    @GetMapping("/")
    public String panel(Model modelo) {
        modelo.addAttribute("panel", armarPanel.ejecutar());
        return "panel";
    }
}
