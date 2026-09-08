package com.example.DWI.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.DWI.model.Formula;

@Repository
public interface FormulaRepository extends JpaRepository<Formula, Long> {

    List<Formula> findTop5ByOrderByIdDesc();

    @Query("SELECT COUNT(DISTINCT f.cliente.id) FROM Formula f WHERE f.cliente IS NOT NULL")
    long contarClientesConFormulas();

    boolean existsByClienteId(Long clienteId);
}
