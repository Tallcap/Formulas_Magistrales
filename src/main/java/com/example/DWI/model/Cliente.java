package com.example.DWI.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "[0-9]{8}", message = "El DNI debe tener exactamente 8 dígitos")
    @Column(nullable = false, unique = true, length = 8)
    private String dni;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden superar 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres")
    @Column(nullable = false, length = 100)
    private String apellidos;

    // Opcional: el cliente puede no proporcionarlo. Si se indica, debe tener
    // 6 dígitos (teléfono fijo) o 9 dígitos (celular).
    @Pattern(regexp = "(\\d{6}|\\d{9})?", message = "El teléfono debe tener 6 dígitos (fijo) o 9 dígitos (celular)")
    @Column(length = 20)
    private String telefono;

    // Opcional: puede no venir de la consulta oficial y el cliente no está obligado a darla.
    @Size(max = 250, message = "La dirección no puede superar 250 caracteres")
    @Column(length = 250)
    private String direccion;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
