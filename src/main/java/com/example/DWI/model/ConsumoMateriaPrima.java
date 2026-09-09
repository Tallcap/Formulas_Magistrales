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
public class ConsumoMateriaPrima {
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @NotNull 
    @ManyToOne(optional=false) 
    @JoinColumn(name="inventario_producto_final_id", nullable=false)
    private InventarioProductoFinal productoFinal;

    @NotNull 
    @ManyToOne(optional=false)
    @JoinColumn(name="inventario_materia_prima_id", nullable=false)
    private InventarioMateriaPrima materiaPrima;
    
    @NotNull 
    @Positive 
    @Column(nullable=false, precision=19, scale=4)
    private BigDecimal cantidad;
}