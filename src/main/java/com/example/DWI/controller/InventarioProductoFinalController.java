package com.example.DWI.controller;

import java.net.URI;
import java.util.List;
import com.example.DWI.model.InventarioProductoFinal;
import com.example.DWI.service.InventarioService;
import com.example.DWI.service.InventarioService.ProduccionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventario/producto-final")
public class InventarioProductoFinalController {
    private final InventarioService service;
    public InventarioProductoFinalController(InventarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<InventarioProductoFinal> listar() {
        return service.listar(InventarioProductoFinal.class);
    }

    @GetMapping("/{id}")
    public InventarioProductoFinal obtener(@PathVariable Long id) {
        return service.obtener(InventarioProductoFinal.class, id);
    }

    @PostMapping
    public ResponseEntity<InventarioProductoFinal> crear(@Valid @RequestBody ProduccionRequest datos) {
        var creado = service.producir(datos);
        return ResponseEntity.created(URI.create("/api/inventario/producto-final/" + creado.getId())).body(creado);
    }
}
