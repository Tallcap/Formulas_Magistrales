package com.example.DWI.controller;

import java.net.URI;
import java.util.List;
import com.example.DWI.model.MateriaPrima;
import com.example.DWI.service.InventarioService;
import com.example.DWI.service.InventarioService.MateriaRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/materias-primas")
public class MateriaPrimaController {
    private final InventarioService service;
    public MateriaPrimaController(InventarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<MateriaPrima> listar() {
        return service.listar(MateriaPrima.class);
    }

    @GetMapping("/{id}")
    public MateriaPrima obtener(@PathVariable Long id) {
        return service.obtener(MateriaPrima.class, id);
    }

    @PostMapping
    public ResponseEntity<MateriaPrima> crear(@Valid @RequestBody MateriaRequest datos) {
        var creado = service.crearMateria(datos);
        return ResponseEntity.created(URI.create("/api/materias-primas/" + creado.getId())).body(creado);
    }
}
