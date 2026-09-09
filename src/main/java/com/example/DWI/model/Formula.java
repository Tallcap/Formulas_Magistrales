package com.example.DWI.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "formulas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Formula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la fórmula es obligatorio")
    @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
    @Column(nullable = false, length = 120)
    private String nombre;

    @NotBlank(message = "La composición es obligatoria")
    @Size(max = 4000, message = "La composición no puede exceder 4000 caracteres")
    @Column(nullable = false, length = 4000)
    private String composicion;

    @NotBlank(message = "Las indicaciones son obligatorias")
    @Size(max = 4000, message = "Las indicaciones no pueden exceder 4000 caracteres")
    @Column(nullable = false, length = 4000)
    private String indicaciones;

    @NotBlank(message = "La presentación es obligatoria")
    @Size(max = 100, message = "La presentación no puede exceder 100 caracteres")
    @Column(length = 100, nullable = false)
    private String presentacion;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "rendimiento_cantidad", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal rendimientoCantidad = BigDecimal.ONE;

    @Column(name = "rendimiento_unidad", nullable = false, length = 30)
    @Builder.Default
    private String rendimientoUnidad = "unidad";

    @Column(name = "contenido_por_envase", precision = 19, scale = 4)
    private BigDecimal contenidoPorEnvase;

    @Column(name = "unidad_contenido", length = 30)
    private String unidadContenido;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    @OneToMany(mappedBy = "formula", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @Builder.Default
    private List<IngredienteFormula> ingredientes = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

     public String getTipoFormula() {
        return cliente == null ? "general" : "personalizada";
    }
}
