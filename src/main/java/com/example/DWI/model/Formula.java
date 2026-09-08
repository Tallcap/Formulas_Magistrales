package com.example.DWI.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
