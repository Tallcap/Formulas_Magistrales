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

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

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
    public Formula obtener(Long id) {
        return formulaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fórmula no encontrada"));
    }

    public Formula guardar(Long id, Formula datos, Long clienteId) {
        Formula formula = (id == null) ? new Formula() : obtener(id);

        if (id != null) {
            em.lock(formula, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            long lotes = em
                    .createQuery("select count(p) from InventarioProductoFinal p where p.formula.id=:id", Long.class)
                    .setParameter("id", id).getSingleResult();
            if (lotes > 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La formula tiene produccion registrada; cree otra formula para modificarla");
        }
        formula.setCliente(clienteId == null ? null : clienteService.obtener(clienteId));
        formula.setNombre(datos.getNombre());
        formula.setPresentacion(datos.getPresentacion());
        formula.setComposicion(datos.getComposicion());
        formula.setIndicaciones(datos.getIndicaciones());

        return formulaRepository.saveAndFlush(formula);
    }

    public Formula cambiarEstado(Long id) {
        Formula formula = obtener(id);
        formula.setActivo(!formula.isActivo());
        return formulaRepository.saveAndFlush(formula);
    }
}
