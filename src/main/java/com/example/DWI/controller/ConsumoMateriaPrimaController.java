package com.example.DWI.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.DWI.model.ConsumoMateriaPrima;
import com.example.DWI.service.InventarioService;

@RestController
@RequestMapping("/api/consumos")
public class ConsumoMateriaPrimaController {

    private final InventarioService service;

    public ConsumoMateriaPrimaController(InventarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConsumoMateriaPrima> listar() {
        return service.listar(ConsumoMateriaPrima.class);
    }

    @GetMapping("/{id}")
    public ConsumoMateriaPrima obtener(@PathVariable Long id) {
        return service.obtener(ConsumoMateriaPrima.class, id);
    }
}