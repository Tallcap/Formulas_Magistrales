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
public class MateriaPrima {
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @NotBlank 
    @Column(nullable=false, unique=true)
    private String nombre;

    @NotBlank 
    @Column(nullable=false)
    private String unidad;

    @NotNull 
    @PositiveOrZero 
    @Column(nullable=false, precision=19, scale=4)
    private BigDecimal stockMinimo = BigDecimal.ZERO;
}