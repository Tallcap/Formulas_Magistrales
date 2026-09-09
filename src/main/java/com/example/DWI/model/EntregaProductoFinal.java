package com.example.DWI.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class EntregaProductoFinal {
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @NotNull 
    @ManyToOne(optional=false) 
    @JoinColumn(name="inventario_producto_final_id", nullable=false)
    private InventarioProductoFinal productoFinal;

    @NotNull 
    @ManyToOne(optional=false) 
    @JoinColumn(name="cliente_id", nullable=false)
    private Cliente cliente;

    @NotNull 
    @Column(nullable=false)
    private LocalDate fecha;

    @Positive 
    @Column(nullable=false)
    private int cantidad;

    @AssertTrue(message="La formula personalizada solo puede entregarse a su cliente")
    
    public boolean isClienteValido() {
        if (productoFinal == null || productoFinal.getFormula() == null || cliente == null) return true;
        Cliente propietario = productoFinal.getFormula().getCliente();
        return propietario == null || propietario == cliente ||
            (propietario.getId() != null && propietario.getId().equals(cliente.getId()));
    }
}