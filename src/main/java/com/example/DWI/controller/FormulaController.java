package com.example.DWI.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.DWI.model.Formula;
import com.example.DWI.service.FormulaService;
import com.example.DWI.service.InventarioService;
import com.example.DWI.service.InventarioService.FormulaImportRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/formulas")
public class FormulaController {

    private final FormulaService formulaService;
    private final InventarioService inventarioService;

    public FormulaController(
            FormulaService formulaService,
            InventarioService inventarioService) {
        this.formulaService = formulaService;
        this.inventarioService = inventarioService;
    }

    public record FormulaRequest(

            @NotBlank(message = "El nombre de la fórmula es obligatorio")
            @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
            String nombre,

            @NotBlank(message = "La presentación es obligatoria")
            @Size(max = 100, message = "La presentación no puede exceder 100 caracteres")
            String presentacion,

            @NotBlank(message = "La composición es obligatoria")
            @Size(max = 4000, message = "La composición no puede exceder 4000 caracteres")
            String composicion,

            @NotBlank(message = "Las indicaciones son obligatorias")
            @Size(max = 4000, message = "Las indicaciones no pueden exceder 4000 caracteres")
            String indicaciones,

            @Positive(message = "El ID del cliente debe ser positivo")
            Long clienteId

    ) {
        public Formula toEntity() {
            Formula formula = new Formula();
            formula.setNombre(nombre);
            formula.setPresentacion(presentacion);
            formula.setComposicion(composicion);
            formula.setIndicaciones(indicaciones);
            return formula;
        }
    }

    @GetMapping
    public List<Formula> listarFormulas() {
        return formulaService.listar();
    }

    @GetMapping("/{id}")
    public Formula obtenerFormula(@PathVariable Long id) {
        return formulaService.obtener(id);
    }

    @PostMapping("/importar")
    public ResponseEntity<Formula> importar(
            @RequestBody FormulaImportRequest datos) {

        Formula nueva = inventarioService.importarFormula(datos);

        return ResponseEntity
                .created(URI.create("/api/formulas/" + nueva.getId()))
                .body(nueva);
    }

    @PutMapping("/{id}")
    public Formula editarFormula(@PathVariable Long id, @Valid @RequestBody FormulaRequest datos) {
            return formulaService.guardar(id, datos.toEntity(), datos.clienteId()
        );
    }

    @DeleteMapping("/{id}")
    public Formula cambiarEstadoDesdeDelete(@PathVariable Long id) {
        return formulaService.cambiarEstado(id);
    }
}