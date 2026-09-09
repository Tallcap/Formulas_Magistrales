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
public class InventarioProductoFinal {
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @NotNull 
    @ManyToOne(optional=false) 
    @JoinColumn(name="formula_id", nullable=false)
    private Formula formula;

    @NotBlank 
    @Column(nullable=false, unique=true)
    private String lote;

    @NotNull 
    @Column(nullable=false)
    private LocalDate fechaProduccion;

    @Positive 
    @Column(nullable=false)
    private int cantidadProducida;

    @PositiveOrZero 
    @Column(nullable=false)
    private int cantidadEntregada;
    
    public int getStockActual() {
        return cantidadProducida - cantidadEntregada;
    }

    @AssertTrue(message="Las entregas no pueden superar la produccion")
    public boolean isStockValido() {
        return getStockActual() >= 0;
    }
}
