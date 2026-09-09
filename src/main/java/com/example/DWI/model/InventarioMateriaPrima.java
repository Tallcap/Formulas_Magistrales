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
public class InventarioMateriaPrima {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "materia_prima_id", nullable = false)
    private MateriaPrima materiaPrima;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String lote;

    @NotNull
    @Column(nullable = false)
    private LocalDate fechaIngreso;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal stockInicial = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal entradas = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal salidasProduccion = BigDecimal.ZERO;

    @Column(name = "stock_actual", nullable = false, precision = 19, scale = 4)
    private BigDecimal stockActual = BigDecimal.ZERO;

    public BigDecimal getStockActual() {
        return stockInicial.add(entradas).subtract(salidasProduccion);
    }

    @PrePersist
    @PreUpdate
    private void sincronizarStockActual() {
        stockActual = getStockActual();
    }

    @AssertTrue(message = "El stock de materia prima no puede ser negativo")
    public boolean isStockValido() {
        return stockInicial == null || entradas == null || salidasProduccion == null || getStockActual().signum() >= 0;
    }
}
