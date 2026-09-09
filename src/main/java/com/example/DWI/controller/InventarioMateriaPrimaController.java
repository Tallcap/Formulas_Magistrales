package com.example.DWI.controller;

import java.net.URI;
import java.util.List;
import com.example.DWI.model.InventarioMateriaPrima;
import com.example.DWI.service.InventarioService;
import com.example.DWI.service.InventarioService.LoteRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventario/materia-prima")
public class InventarioMateriaPrimaController {
    private final InventarioService service;
    public InventarioMateriaPrimaController(InventarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<InventarioMateriaPrima> listar() {
        return service.listar(InventarioMateriaPrima.class);
    }

    @GetMapping("/{id}")
    public InventarioMateriaPrima obtener(@PathVariable Long id) {
        return service.obtener(InventarioMateriaPrima.class, id);
    }

    @PostMapping
    public ResponseEntity<InventarioMateriaPrima> crear(@Valid @RequestBody LoteRequest datos) {
        var creado = service.ingresarLote(datos);
        return ResponseEntity.created(URI.create("/api/inventario/materia-prima/" + creado.getId())).body(creado);
    }

    @PostMapping("/masivo")
    public ResponseEntity<List<InventarioMateriaPrima>> crearMasivo(@RequestBody @jakarta.validation.constraints.NotEmpty List<@Valid LoteRequest> datos) {
        var creados = service.ingresarLotes(datos);
        return ResponseEntity.ok(creados);
    }
}
