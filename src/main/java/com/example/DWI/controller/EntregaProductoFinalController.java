package com.example.DWI.controller;

import java.net.URI;
import java.util.List;
import com.example.DWI.model.EntregaProductoFinal;
import com.example.DWI.service.InventarioService;
import com.example.DWI.service.InventarioService.EntregaRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entregas")
public class EntregaProductoFinalController {
    private final InventarioService service;
    public EntregaProductoFinalController(InventarioService service) { 
        this.service = service; }

    @GetMapping 
    public List<EntregaProductoFinal> listar() { 
        return service.listar(EntregaProductoFinal.class); }

    @GetMapping("/{id}") 
    public EntregaProductoFinal obtener(@PathVariable Long id) { 
        return service.obtener(EntregaProductoFinal.class, id); }

    @PostMapping 
    public ResponseEntity<EntregaProductoFinal> crear(@Valid @RequestBody EntregaRequest datos) {
        var creado = service.entregar(datos);
        return ResponseEntity.created(URI.create("/api/entregas/" + creado.getId())).body(creado);
    }
}