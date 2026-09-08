package com.example.DWI.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.DWI.model.Cliente;
import com.example.DWI.repository.ClienteRepository;
import com.example.DWI.repository.FormulaRepository;

@Service
@Transactional
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final FormulaRepository formulaRepository;

    public ClienteService(ClienteRepository clienteRepository, FormulaRepository formulaRepository) {
        this.clienteRepository = clienteRepository;
        this.formulaRepository = formulaRepository;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public long contar() {
        return clienteRepository.count();
    }

    @Transactional(readOnly = true)
    public Cliente obtener(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    public Cliente guardar(Long id, Cliente datos) {
        if (id == null) {
            // Alta: se registran todos los datos y se valida que el DNI no exista.
            if (clienteRepository.existsByDni(datos.getDni())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente registrado con este DNI");
            }

            Cliente cliente = new Cliente();
            cliente.setDni(datos.getDni());
            cliente.setNombres(datos.getNombres());
            cliente.setApellidos(datos.getApellidos());
            cliente.setTelefono(normalizarOpcional(datos.getTelefono()));
            cliente.setDireccion(normalizarOpcional(datos.getDireccion()));
            return clienteRepository.saveAndFlush(cliente);
        }

        // Edición: el DNI, los nombres y los apellidos son datos de identidad y no
        // pueden modificarse; solo se actualizan el teléfono y la dirección.
        Cliente cliente = obtener(id);
        cliente.setTelefono(normalizarOpcional(datos.getTelefono()));
        cliente.setDireccion(normalizarOpcional(datos.getDireccion()));
        return clienteRepository.saveAndFlush(cliente);
    }

    /** Convierte cadenas vacías o en blanco en null para los campos opcionales (teléfono y dirección). */
    private static String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    public void eliminar(Long id) {
        Cliente cliente = obtener(id);
        if (formulaRepository.existsByClienteId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El cliente tiene fórmulas asociadas y no puede ser eliminado");
        }
        clienteRepository.delete(cliente);
        clienteRepository.flush();
    }

    public Cliente cambiarEstado(Long id) {
        Cliente cliente = obtener(id);
        cliente.setActivo(!cliente.isActivo());
        return clienteRepository.saveAndFlush(cliente);
    }
}
