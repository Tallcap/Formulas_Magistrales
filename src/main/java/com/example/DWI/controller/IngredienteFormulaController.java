package com.example.DWI.controller;

import java.util.List;
import com.example.DWI.model.IngredienteFormula;
import com.example.DWI.service.InventarioService;
import com.example.DWI.service.InventarioService.IngredienteRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/formulas/{formulaId}/ingredientes")
public class IngredienteFormulaController {
    private final InventarioService service;

    public IngredienteFormulaController(InventarioService service) { this.service = service; }

    @GetMapping 
    public List<IngredienteFormula> listar(@PathVariable Long formulaId) {
        return service.ingredientes(formulaId);
    }
    
    @PutMapping 
    public List<IngredienteFormula> definir(@PathVariable Long formulaId,
            @RequestBody @NotEmpty List<@Valid IngredienteRequest> datos) {
        return service.definirIngredientes(formulaId, datos);
    }
}