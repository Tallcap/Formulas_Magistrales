package com.example.DWI.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class IngredienteFormula {
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @NotNull 
    @ManyToOne(optional=false) 
    @JoinColumn(name="formula_id", nullable=false) 
    @JsonBackReference
    private Formula formula;

    @NotNull 
    @ManyToOne(optional=false)
    @JoinColumn(name="materia_prima_id", nullable=false)
    private MateriaPrima materiaPrima;


    @NotNull 
    @Positive 
    @Column(nullable=false, precision=19, scale=4)
    private BigDecimal cantidad;

    @NotBlank 
    @Column(nullable=false, length=30)
    private String unidad = "g";
}
