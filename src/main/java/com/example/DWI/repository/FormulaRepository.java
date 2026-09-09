package com.example.DWI.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.DWI.model.Formula;

@Repository
public interface FormulaRepository extends JpaRepository<Formula, Long> {

    boolean existsByClienteId(Long clienteId);
}
