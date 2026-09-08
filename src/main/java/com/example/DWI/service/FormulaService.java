package com.example.DWI.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.DWI.model.Formula;
import com.example.DWI.repository.FormulaRepository;

@Service
@Transactional
public class FormulaService {

    private final FormulaRepository formulaRepository;
    private final ClienteService clienteService;

    public FormulaService(FormulaRepository formulaRepository, ClienteService clienteService) {
        this.formulaRepository = formulaRepository;
        this.clienteService = clienteService;
    }

    @Transactional(readOnly = true)
    public List<Formula> listar() {
        return formulaRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public long contar() {
        return formulaRepository.count();
    }

    @Transactional(readOnly = true)
    public long contarClientesConFormulas() {
        return formulaRepository.contarClientesConFormulas();
    }

    @Transactional(readOnly = true)
    public List<Formula> recientes() {
        return formulaRepository.findTop5ByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Formula obtener(Long id) {
        return formulaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fórmula no encontrada"));
    }

    public Formula guardar(Long id, Formula datos, Long clienteId) {
        Formula formula = (id == null) ? new Formula() : obtener(id);

        formula.setCliente(clienteId == null ? null : clienteService.obtener(clienteId));
        formula.setNombre(datos.getNombre());
        formula.setPresentacion(datos.getPresentacion());
        formula.setComposicion(datos.getComposicion());
        formula.setIndicaciones(datos.getIndicaciones());

        return formulaRepository.saveAndFlush(formula);
    }

    public void eliminar(Long id) {
        Formula formula = obtener(id);
        formulaRepository.delete(formula);
        formulaRepository.flush();
    }

    public Formula cambiarEstado(Long id) {
        Formula formula = obtener(id);
        formula.setActivo(!formula.isActivo());
        return formulaRepository.saveAndFlush(formula);
    }
}
