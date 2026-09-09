package com.example.DWI.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import com.example.DWI.model.Cliente;
import com.example.DWI.model.ConsumoMateriaPrima;
import com.example.DWI.model.EntregaProductoFinal;
import com.example.DWI.model.Formula;
import com.example.DWI.model.IngredienteFormula;
import com.example.DWI.model.InventarioMateriaPrima;
import com.example.DWI.model.InventarioProductoFinal;
import com.example.DWI.model.MateriaPrima;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Service
@Validated
@Transactional
public class InventarioService {
    @PersistenceContext
    private EntityManager em;

    public record MateriaRequest(
            @NotBlank @Size(max = 255) String nombre,
            @NotBlank @Size(max = 255) String unidad,
            @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal stockMinimo) {
    }

    public record IngredienteRequest(
            @NotNull @Positive Long materiaPrimaId,
            @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal cantidad) {
    }

    public record LoteRequest(
            @NotNull @Positive Long materiaPrimaId,
            @NotBlank @Size(max = 255) String lote,
            @NotNull @PastOrPresent LocalDate fechaIngreso,
            @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal cantidad) {
    }

    public record ProduccionRequest(
            @NotNull @Positive Long formulaId,
            @NotBlank String lote,
            @NotNull @PastOrPresent LocalDate fechaProduccion,
            @Positive int cantidadProducida,
            @NotEmpty List<@NotNull @Positive Long> lotesMateriaPrimaIds) {
    }

    public record EntregaRequest(
            @NotNull @Positive Long productoFinalId,
            @NotNull @Positive Long clienteId,
            @NotNull @PastOrPresent LocalDate fecha,
            @Positive int cantidad) {
    }

    public record RendimientoRequest(
            Integer cantidad,
            String unidad,
            BigDecimal contenido_por_envase,
            String unidad_contenido) {
    }

    public record IngredienteImportRequest(
            String id_materia_prima,
            BigDecimal cantidad,
            String unidad) {
    }

    public record FormulaImportRequest(
            String id_formula,
            String id_cliente,
            String nombre,
            Integer version,
            String presentacion,
            RendimientoRequest rendimiento,
            List<IngredienteImportRequest> ingredientes,
            String indicaciones,
            Boolean activo,
            String tipo_formula) {
    }

    public Formula importarFormula(FormulaImportRequest datos) {
        Formula f = new Formula();
        f.setNombre(datos.nombre());
        f.setPresentacion(datos.presentacion());
        f.setIndicaciones(datos.indicaciones());
        f.setActivo(datos.activo() == null || datos.activo());
        f.setVersion(datos.version() == null ? 1 : datos.version());
        if (datos.rendimiento() != null) {
            var r = datos.rendimiento();
            f.setRendimientoCantidad(BigDecimal.valueOf(r.cantidad() == null ? 1 : r.cantidad()));
            f.setRendimientoUnidad(r.unidad());
            f.setContenidoPorEnvase(r.contenido_por_envase());
            f.setUnidadContenido(r.unidad_contenido());
        }
        String composicion = datos.ingredientes() == null ? "Sin ingredientes"
                : datos.ingredientes().stream()
                        .map(x -> "Materia prima " + x.id_materia_prima() + ": " + x.cantidad() + " " + x.unidad())
                        .reduce((a, b) -> a + ", " + b).orElse("Sin ingredientes");
        f.setComposicion(composicion);
        if (datos.id_cliente() != null)
            f.setCliente(obtener(Cliente.class, Long.valueOf(datos.id_cliente())));
        em.persist(f);
        em.flush();
        if (datos.ingredientes() != null)
            for (var x : datos.ingredientes()) {
                var i = new IngredienteFormula();
                i.setFormula(f);
                i.setCantidad(x.cantidad());
                i.setUnidad(x.unidad());
                i.setMateriaPrima(obtener(MateriaPrima.class, Long.valueOf(x.id_materia_prima())));
                em.persist(i);
                f.getIngredientes().add(i);
            }
        em.flush();
        return f;
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    @Transactional(readOnly = true)
    public <T> T obtener(Class<T> tipo, Long id) {
        T valor = em.find(tipo, id);
        if (valor == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, tipo.getSimpleName() + " no encontrado");
        return valor;
    }

    private <T> T bloquear(Class<T> tipo, Long id) {
        T valor = em.find(tipo, id, LockModeType.PESSIMISTIC_WRITE);
        if (valor == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, tipo.getSimpleName() + " no encontrado");
        return valor;
    }

    @Transactional(readOnly = true)
    public <T> List<T> listar(Class<T> tipo) {
        return em.createQuery("select e from " + tipo.getSimpleName() + " e order by e.id", tipo).getResultList();
    }

    public MateriaPrima crearMateria(@Valid MateriaRequest datos) {
        MateriaPrima m = new MateriaPrima();
        m.setNombre(datos.nombre().trim());
        m.setUnidad(datos.unidad().trim());
        m.setStockMinimo(datos.stockMinimo());
        em.persist(m);
        em.flush();
        return m;
    }

    // Toda la lista se guarda en la misma transaccion: cualquier fallo revierte la
    // carga.
    public List<MateriaPrima> crearMaterias(@NotEmpty List<@NotNull @Valid MateriaRequest> datos) {
        Set<String> nombres = new HashSet<>();
        for (var dato : datos) {
            String nombre = dato.nombre().trim();
            if (!nombres.add(nombre.toLowerCase(Locale.ROOT))) {
                throw conflicto("Materia prima repetida en la lista: " + nombre);
            }
            long existentes = em.createQuery(
                    "select count(m) from MateriaPrima m where lower(trim(m.nombre)) = :nombre", Long.class)
                    // convierte a minusculas y elimina espacios en blanco para evitar duplicados
                    .setParameter("nombre", nombre.toLowerCase(Locale.ROOT)).getSingleResult();
            if (existentes > 0)
                throw conflicto("La materia prima ya existe: " + nombre);
        }
        List<MateriaPrima> resultado = new ArrayList<>();
        for (var dato : datos)
            resultado.add(crearMateria(dato));
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<IngredienteFormula> ingredientes(Long formulaId) {
        obtener(Formula.class, formulaId);
        return em
                .createQuery("select i from IngredienteFormula i where i.formula.id=:id order by i.id",
                        IngredienteFormula.class)
                .setParameter("id", formulaId).getResultList();
    }

    // Las cantidades corresponden a un envase de la presentacion de la formula.
    public List<IngredienteFormula> definirIngredientes(Long formulaId,
            @NotEmpty List<@Valid IngredienteRequest> datos) {
        Formula f = bloquear(Formula.class, formulaId);
        if (em.createQuery("select count(p) from InventarioProductoFinal p where p.formula.id=:id", Long.class)
                .setParameter("id", formulaId).getSingleResult() > 0)
            throw conflicto("La formula ya tiene produccion; cree otra formula para cambiar su composicion");
        Set<Long> ids = new HashSet<>();
        for (var d : datos) {
            if (!ids.add(d.materiaPrimaId()))
                throw conflicto("Materia prima repetida");
            obtener(MateriaPrima.class, d.materiaPrimaId());
        }
        for (var anterior : ingredientes(formulaId))
            em.remove(anterior);
        em.flush();
        List<IngredienteFormula> resultado = new ArrayList<>();
        for (var d : datos) {
            var i = new IngredienteFormula();
            i.setFormula(f);
            i.setMateriaPrima(obtener(MateriaPrima.class, d.materiaPrimaId()));
            i.setCantidad(d.cantidad());
            em.persist(i);
            resultado.add(i);
        }
        em.flush();
        return resultado;
    }

    public List<InventarioMateriaPrima> ingresarLotes(@NotEmpty List<@NotNull @Valid LoteRequest> datos) {
        Set<String> codigos = new HashSet<>();
        for (var dato : datos) {
            String codigo = dato.lote().trim();
            if (!codigos.add(codigo.toLowerCase(Locale.ROOT))) {
                throw conflicto("Lote repetido en la lista: " + codigo);
            }
            long existentes = em.createQuery(
                    "select count(l) from InventarioMateriaPrima l where lower(trim(l.lote)) = :codigo", Long.class)
                    .setParameter("codigo", codigo.toLowerCase(Locale.ROOT)).getSingleResult();
            if (existentes > 0)
                throw conflicto("El lote ya existe: " + codigo);
            obtener(MateriaPrima.class, dato.materiaPrimaId());
        }
        List<InventarioMateriaPrima> resultado = new ArrayList<>();
        for (var dato : datos)
            resultado.add(ingresarLote(dato));
        return resultado;
    }

    public InventarioMateriaPrima ingresarLote(@Valid LoteRequest datos) {
        var m = obtener(MateriaPrima.class, datos.materiaPrimaId());
        var l = new InventarioMateriaPrima();
        l.setMateriaPrima(m);
        l.setLote(datos.lote().trim());
        l.setFechaIngreso(datos.fechaIngreso());
        l.setStockInicial(datos.cantidad());
        em.persist(l);
        em.flush();
        return l;
    }

    public InventarioProductoFinal producir(@Valid ProduccionRequest datos) {
        Formula f = bloquear(Formula.class, datos.formulaId());
        if (!f.isActivo())
            throw conflicto("La formula esta inactiva");
        var ingredientes = ingredientes(f.getId());
        if (ingredientes.isEmpty())
            throw conflicto("La formula no tiene ingredientes registrados");
        var ids = new TreeSet<>(datos.lotesMateriaPrimaIds());
        if (ids.size() != datos.lotesMateriaPrimaIds().size())
            throw conflicto("Lote repetido");
        Map<Long, List<InventarioMateriaPrima>> disponibles = new HashMap<>();
        Set<Long> materias = new HashSet<>();
        for (var i : ingredientes)
            materias.add(i.getMateriaPrima().getId());
        for (Long id : ids) {
            var l = bloquear(InventarioMateriaPrima.class, id);
            if (!materias.contains(l.getMateriaPrima().getId()))
                throw conflicto("Lote ajeno a la formula");
            if (l.getFechaIngreso().isAfter(datos.fechaProduccion()))
                throw conflicto("El lote ingreso despues de la produccion");
            disponibles.computeIfAbsent(l.getMateriaPrima().getId(), k -> new ArrayList<>()).add(l);
        }
        var p = new InventarioProductoFinal();
        p.setFormula(f);
        p.setLote(datos.lote().trim());
        p.setFechaProduccion(datos.fechaProduccion());
        p.setCantidadProducida(datos.cantidadProducida());
        em.persist(p);
        for (var i : ingredientes) {
            BigDecimal pendiente = i.getCantidad().multiply(BigDecimal.valueOf(datos.cantidadProducida()));
            for (var l : disponibles.getOrDefault(i.getMateriaPrima().getId(), List.of())) {
                BigDecimal cantidad = pendiente.min(l.getStockActual());
                if (cantidad.signum() <= 0)
                    continue;
                l.setSalidasProduccion(l.getSalidasProduccion().add(cantidad));
                var uso = new ConsumoMateriaPrima();
                uso.setProductoFinal(p);
                uso.setMateriaPrima(l);
                uso.setCantidad(cantidad);
                em.persist(uso);
                pendiente = pendiente.subtract(cantidad);
            }
            if (pendiente.signum() > 0)
                throw conflicto("Stock insuficiente para " + i.getMateriaPrima().getNombre());
        }
        em.flush();
        return p;
    }

    public EntregaProductoFinal entregar(@Valid EntregaRequest datos) {
        var p = bloquear(InventarioProductoFinal.class, datos.productoFinalId());
        var c = obtener(Cliente.class, datos.clienteId());
        if (!c.isActivo())
            throw conflicto("El cliente esta inactivo");
        if (datos.fecha().isBefore(p.getFechaProduccion()))
            throw conflicto("La entrega es anterior a la produccion");
        var e = new EntregaProductoFinal();
        e.setProductoFinal(p);
        e.setCliente(c);
        e.setFecha(datos.fecha());
        e.setCantidad(datos.cantidad());
        if (!e.isClienteValido())
            throw conflicto("La formula personalizada pertenece a otro cliente");
        if (datos.cantidad() > p.getStockActual())
            throw conflicto("Stock de producto final insuficiente");
        p.setCantidadEntregada(p.getCantidadEntregada() + datos.cantidad());
        em.persist(e);
        em.flush();
        return e;
    }
}
