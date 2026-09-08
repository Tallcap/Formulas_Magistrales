package com.example.DWI.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.DWI.model.Cliente;
import com.example.DWI.service.ApiCloudService;
import com.example.DWI.service.ClienteService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final ApiCloudService apiCloudService;

    public ClienteController(ClienteService clienteService, ApiCloudService apiCloudService) {
        this.clienteService = clienteService;
        this.apiCloudService = apiCloudService;
    }

    public record ClienteRequest(
            @NotBlank(message = "El DNI es obligatorio")
            @Pattern(regexp = "[0-9]{8}", message = "El DNI debe tener 8 dígitos")
            String dni,

            @NotBlank(message = "Los nombres son obligatorios")
            @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
            String nombres,

            @NotBlank(message = "Los apellidos son obligatorios")
            @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
            String apellidos,

            @Pattern(regexp = "(\\d{6}|\\d{9})?", message = "El teléfono debe tener 6 dígitos (fijo) o 9 dígitos (celular)")
            String telefono,

            @Size(max = 250, message = "La dirección no puede exceder 250 caracteres")
            String direccion
    ) {
        public Cliente toEntity() {
            Cliente c = new Cliente();
            c.setDni(dni);
            c.setNombres(nombres);
            c.setApellidos(apellidos);
            c.setTelefono(telefono);
            c.setDireccion(direccion);
            return c;
        }
    }

    @GetMapping("/dni/{dni}")
    public Map<String, Object> consultarDni(@PathVariable String dni) {
        return apiCloudService.consultar(dni);
    }

    @GetMapping
    public List<Cliente> listarClientes() {
        return clienteService.listar();
    }

    @GetMapping("/{id}")
    public Cliente obtenerCliente(@PathVariable Long id) {
        return clienteService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Cliente> crearCliente(@Valid @RequestBody ClienteRequest datos) {
        Cliente nuevo = clienteService.guardar(null, datos.toEntity());
        return ResponseEntity.created(URI.create("/api/clientes/" + nuevo.getId())).body(nuevo);
    }

    @PutMapping("/{id}")
    public Cliente editarCliente(@PathVariable Long id, @Valid @RequestBody ClienteRequest datos) {
        return clienteService.guardar(id, datos.toEntity());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    public Cliente cambiarEstado(@PathVariable Long id) {
        return clienteService.cambiarEstado(id);
    }
}
