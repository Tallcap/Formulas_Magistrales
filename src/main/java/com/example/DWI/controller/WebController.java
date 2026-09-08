package com.example.DWI.controller;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.DWI.model.Cliente;
import com.example.DWI.model.Formula;
import com.example.DWI.service.ApiCloudService;
import com.example.DWI.service.ClienteService;
import com.example.DWI.service.FormulaService;

import jakarta.validation.Valid;

/**
 * Controlador principal de vistas web (Thymeleaf).
 * Gestiona la navegación del Dashboard, el módulo de Clientes (con consulta RENIEC vía ApiCloud)
 * y el módulo de Fórmulas Magistrales.
 */
@Controller
public class WebController {

    private final ClienteService clienteService;
    private final FormulaService formulaService;
    private final ApiCloudService apiCloudService;

    public WebController(
            ClienteService clienteService,
            FormulaService formulaService,
            ApiCloudService apiCloudService
    ) {
        this.clienteService = clienteService;
        this.formulaService = formulaService;
        this.apiCloudService = apiCloudService;
    }

    // =========================================================================
    // CONSULTA DNI EN TIEMPO REAL (APICLOUD / RENIEC)
    // =========================================================================

    /**
     * Endpoint consultado por JavaScript (modales.js) para obtener datos oficiales del DNI.
     */
    @GetMapping("/clientes/dni/{dni}")
    @ResponseBody
    public Map<String, Object> consultarDni(@PathVariable String dni) {
        return apiCloudService.consultar(dni);
    }

    // =========================================================================
    // NAVEGACIÓN GENERAL Y DASHBOARD
    // =========================================================================

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalClientes", clienteService.contar());
        model.addAttribute("totalFormulas", formulaService.contar());
        model.addAttribute("clientesConFormulas", formulaService.contarClientesConFormulas());
        model.addAttribute("ultimasFormulas", formulaService.recientes());
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // =========================================================================
    // MÓDULO DE CLIENTES
    // =========================================================================

    @GetMapping("/clientes")
    public String listarClientes(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        if (!model.containsAttribute("cliente")) {
            model.addAttribute("cliente", new Cliente());
        }
        return "clientes";
    }

    @GetMapping("/clientes/nuevo")
    public String modalNuevoCliente(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("abrirModal", true);
        return listarClientes(model);
    }

    @GetMapping("/clientes/{id}")
    public String verCliente(@PathVariable Long id, Model model) {
        model.addAttribute("cliente", clienteService.obtener(id));
        return "cliente-detalle";
    }

    @GetMapping("/clientes/{id}/editar")
    public String modalEditarCliente(@PathVariable Long id, Model model) {
        model.addAttribute("cliente", clienteService.obtener(id));
        model.addAttribute("abrirModal", true);
        return listarClientes(model);
    }

    @PostMapping("/clientes/guardar")
    public String guardarCliente(
            @Valid @ModelAttribute("cliente") Cliente cliente,
            BindingResult errors,
            Model model,
            RedirectAttributes flash
    ) {
        if (errors.hasErrors()) {
            model.addAttribute("abrirModal", true);
            return listarClientes(model);
        }

        try {
            clienteService.guardar(cliente.getId(), cliente);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == 409) {
                errors.rejectValue("dni", "duplicate", e.getReason());
                model.addAttribute("abrirModal", true);
                return listarClientes(model);
            }
            throw e;
        } catch (DataIntegrityViolationException e) {
            errors.rejectValue("dni", "duplicate", "Ya existe un cliente con este DNI en la base de datos");
            model.addAttribute("abrirModal", true);
            return listarClientes(model);
        }

        flash.addFlashAttribute("mensaje", "Cliente guardado correctamente");
        return "redirect:/clientes";
    }

    @PostMapping("/clientes/{id}/eliminar")
    public String eliminarCliente(@PathVariable Long id, RedirectAttributes flash) {
        try {
            clienteService.eliminar(id);
            flash.addFlashAttribute("mensaje", "Cliente eliminado correctamente");
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == 409) {
                flash.addFlashAttribute("error", e.getReason());
            } else {
                throw e;
            }
        } catch (DataIntegrityViolationException e) {
            flash.addFlashAttribute("error", "No se puede eliminar el cliente porque tiene fórmulas asociadas");
        }
        return "redirect:/clientes";
    }

    @PostMapping("/clientes/{id}/estado")
    public String cambiarEstadoCliente(@PathVariable Long id) {
        clienteService.cambiarEstado(id);
        return "redirect:/clientes";
    }

    // =========================================================================
    // MÓDULO DE FÓRMULAS MAGISTRALES
    // =========================================================================

    @GetMapping("/formulas")
    public String listarFormulas(Model model) {
        model.addAttribute("formulas", formulaService.listar());
        cargarOpcionesClientes(model);
        if (!model.containsAttribute("formula")) {
            model.addAttribute("formula", new Formula());
        }
        return "formulas";
    }

    @GetMapping("/formulas/nueva")
    public String modalNuevaFormula(Model model) {
        model.addAttribute("formula", new Formula());
        cargarOpcionesClientes(model);
        model.addAttribute("abrirModal", true);
        return listarFormulas(model);
    }

    @GetMapping("/formulas/{id}")
    public String verFormula(@PathVariable Long id, Model model) {
        model.addAttribute("formula", formulaService.obtener(id));
        return "formula-detalle";
    }

    @GetMapping("/formulas/{id}/editar")
    public String modalEditarFormula(@PathVariable Long id, Model model) {
        model.addAttribute("formula", formulaService.obtener(id));
        cargarOpcionesClientes(model);
        model.addAttribute("abrirModal", true);
        return listarFormulas(model);
    }

    @PostMapping("/formulas/guardar")
    public String guardarFormula(
            @Valid @ModelAttribute("formula") Formula formula,
            BindingResult errors,
            Model model,
            RedirectAttributes flash
    ) {
        if (errors.hasErrors()) {
            cargarOpcionesClientes(model);
            model.addAttribute("abrirModal", true);
            return listarFormulas(model);
        }

        try {
            Long clienteId = formula.getCliente() != null ? formula.getCliente().getId() : null;
            formulaService.guardar(formula.getId(), formula, clienteId);
        } catch (ResponseStatusException e) {
            errors.rejectValue("cliente", "invalid", e.getReason());
            cargarOpcionesClientes(model);
            model.addAttribute("abrirModal", true);
            return listarFormulas(model);
        }

        flash.addFlashAttribute("mensaje", "Fórmula magistral guardada correctamente");
        return "redirect:/formulas";
    }

    @PostMapping("/formulas/{id}/eliminar")
    public String eliminarFormula(@PathVariable Long id, RedirectAttributes flash) {
        formulaService.eliminar(id);
        flash.addFlashAttribute("mensaje", "Fórmula magistral eliminada");
        return "redirect:/formulas";
    }

    @PostMapping("/formulas/{id}/estado")
    public String cambiarEstadoFormula(@PathVariable Long id) {
        formulaService.cambiarEstado(id);
        return "redirect:/formulas";
    }

    // =========================================================================
    // UTILIDADES INTERNAS
    // =========================================================================

    private void cargarOpcionesClientes(Model model) {
        model.addAttribute("clientes", clienteService.listar());
    }
}

