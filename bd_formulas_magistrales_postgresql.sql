-- Base de datos para la gestion de formulas magistrales
-- Motor: PostgreSQL

BEGIN;

CREATE SCHEMA IF NOT EXISTS seguridad;
CREATE SCHEMA IF NOT EXISTS inventario;
CREATE SCHEMA IF NOT EXISTS produccion;
CREATE SCHEMA IF NOT EXISTS compras;
CREATE SCHEMA IF NOT EXISTS comercial;

-- =========================================================
-- SEGURIDAD
-- =========================================================

CREATE TABLE seguridad.usuarios (
    id_usuario  BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    correo      VARCHAR(254) NOT NULL UNIQUE,
    clave_hash  VARCHAR(255) NOT NULL,
    rol         VARCHAR(50) NOT NULL,
    activo      BOOLEAN NOT NULL DEFAULT TRUE
);

-- =========================================================
-- INVENTARIO
-- =========================================================

CREATE TABLE inventario.productos (
    id_producto    BIGSERIAL PRIMARY KEY,
    codigo         VARCHAR(50) NOT NULL UNIQUE,
    nombre         VARCHAR(200) NOT NULL,
    tipo           VARCHAR(50) NOT NULL,
    unidad_medida  VARCHAR(30) NOT NULL,
    stock_minimo   NUMERIC(14,3) NOT NULL DEFAULT 0,
    precio_venta   NUMERIC(14,2),
    activo         BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_productos_stock_minimo
        CHECK (stock_minimo >= 0),
    CONSTRAINT ck_productos_precio_venta
        CHECK (precio_venta IS NULL OR precio_venta >= 0)
);

CREATE TABLE inventario.lotes (
    id_lote              BIGSERIAL PRIMARY KEY,
    id_producto          BIGINT NOT NULL,
    numero_lote          VARCHAR(80) NOT NULL,
    fecha_ingreso        DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_elaboracion    DATE,
    fecha_vencimiento    DATE,
    cantidad_inicial     NUMERIC(14,3) NOT NULL,
    cantidad_disponible  NUMERIC(14,3) NOT NULL,
    costo_unitario       NUMERIC(14,4) NOT NULL,
    estado               VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
    CONSTRAINT fk_lotes_producto
        FOREIGN KEY (id_producto)
        REFERENCES inventario.productos (id_producto),
    CONSTRAINT uq_lotes_producto_numero
        UNIQUE (id_producto, numero_lote),
    CONSTRAINT ck_lotes_fechas
        CHECK (fecha_vencimiento IS NULL
               OR fecha_elaboracion IS NULL
               OR fecha_vencimiento >= fecha_elaboracion),
    CONSTRAINT ck_lotes_cantidades
        CHECK (cantidad_inicial >= 0
               AND cantidad_disponible >= 0
               AND cantidad_disponible <= cantidad_inicial),
    CONSTRAINT ck_lotes_costo
        CHECK (costo_unitario >= 0)
);

CREATE TABLE inventario.movimientos_inventario (
    id_movimiento    BIGSERIAL PRIMARY KEY,
    id_lote          BIGINT NOT NULL,
    id_usuario       BIGINT NOT NULL,
    tipo_movimiento  VARCHAR(50) NOT NULL,
    naturaleza       CHAR(1) NOT NULL,
    cantidad         NUMERIC(14,3) NOT NULL,
    stock_anterior   NUMERIC(14,3) NOT NULL,
    stock_posterior  NUMERIC(14,3) NOT NULL,
    tipo_referencia  VARCHAR(50),
    id_referencia    BIGINT,
    fecha_movimiento TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimientos_lote
        FOREIGN KEY (id_lote)
        REFERENCES inventario.lotes (id_lote),
    CONSTRAINT fk_movimientos_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES seguridad.usuarios (id_usuario),
    CONSTRAINT ck_movimientos_naturaleza
        CHECK (naturaleza IN ('E', 'S')),
    CONSTRAINT ck_movimientos_cantidad
        CHECK (cantidad > 0),
    CONSTRAINT ck_movimientos_stock
        CHECK (stock_anterior >= 0 AND stock_posterior >= 0),
    CONSTRAINT ck_movimientos_referencia
        CHECK ((tipo_referencia IS NULL AND id_referencia IS NULL)
            OR (tipo_referencia IS NOT NULL AND id_referencia IS NOT NULL))
);

-- =========================================================
-- PRODUCCION
-- =========================================================

CREATE TABLE produccion.formulas (
    id_formula             BIGSERIAL PRIMARY KEY,
    id_producto_terminado  BIGINT NOT NULL,
    codigo                 VARCHAR(50) NOT NULL,
    nombre                 VARCHAR(200) NOT NULL,
    version                VARCHAR(30) NOT NULL,
    rendimiento            NUMERIC(14,3) NOT NULL,
    procedimiento          TEXT NOT NULL,
    activa                 BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_formulas_producto_terminado
        FOREIGN KEY (id_producto_terminado)
        REFERENCES inventario.productos (id_producto),
    CONSTRAINT uq_formulas_codigo_version
        UNIQUE (codigo, version),
    CONSTRAINT ck_formulas_rendimiento
        CHECK (rendimiento > 0)
);

CREATE TABLE produccion.detalle_formula (
    id_detalle      BIGSERIAL PRIMARY KEY,
    id_formula      BIGINT NOT NULL,
    id_insumo       BIGINT NOT NULL,
    cantidad        NUMERIC(14,3) NOT NULL,
    unidad_medida   VARCHAR(30) NOT NULL,
    porcentaje      NUMERIC(7,4),
    observacion     VARCHAR(500),
    CONSTRAINT fk_detalle_formula_formula
        FOREIGN KEY (id_formula)
        REFERENCES produccion.formulas (id_formula)
        ON DELETE CASCADE,
    CONSTRAINT fk_detalle_formula_insumo
        FOREIGN KEY (id_insumo)
        REFERENCES inventario.productos (id_producto),
    CONSTRAINT uq_detalle_formula_insumo
        UNIQUE (id_formula, id_insumo),
    CONSTRAINT ck_detalle_formula_cantidad
        CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_formula_porcentaje
        CHECK (porcentaje IS NULL OR porcentaje BETWEEN 0 AND 100)
);

CREATE TABLE produccion.ordenes_produccion (
    id_orden               BIGSERIAL PRIMARY KEY,
    id_formula             BIGINT NOT NULL,
    id_responsable         BIGINT NOT NULL,
    numero_orden           VARCHAR(50) NOT NULL UNIQUE,
    cantidad_planificada   NUMERIC(14,3) NOT NULL,
    cantidad_obtenida      NUMERIC(14,3),
    estado                 VARCHAR(30) NOT NULL DEFAULT 'PLANIFICADA',
    fecha_inicio           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_finalizacion     TIMESTAMPTZ,
    CONSTRAINT fk_ordenes_formula
        FOREIGN KEY (id_formula)
        REFERENCES produccion.formulas (id_formula),
    CONSTRAINT fk_ordenes_responsable
        FOREIGN KEY (id_responsable)
        REFERENCES seguridad.usuarios (id_usuario),
    CONSTRAINT ck_ordenes_cantidades
        CHECK (cantidad_planificada > 0
               AND (cantidad_obtenida IS NULL OR cantidad_obtenida >= 0)),
    CONSTRAINT ck_ordenes_fechas
        CHECK (fecha_finalizacion IS NULL
               OR fecha_finalizacion >= fecha_inicio)
);

CREATE TABLE produccion.consumos_produccion (
    id_consumo           BIGSERIAL PRIMARY KEY,
    id_orden             BIGINT NOT NULL,
    id_lote              BIGINT NOT NULL,
    cantidad_planificada NUMERIC(14,3) NOT NULL,
    cantidad_consumida   NUMERIC(14,3) NOT NULL,
    fecha_consumo        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacion          VARCHAR(500),
    CONSTRAINT fk_consumos_orden
        FOREIGN KEY (id_orden)
        REFERENCES produccion.ordenes_produccion (id_orden)
        ON DELETE CASCADE,
    CONSTRAINT fk_consumos_lote
        FOREIGN KEY (id_lote)
        REFERENCES inventario.lotes (id_lote),
    CONSTRAINT ck_consumos_cantidades
        CHECK (cantidad_planificada > 0 AND cantidad_consumida >= 0)
);

CREATE TABLE produccion.resultados_produccion (
    id_resultado       BIGSERIAL PRIMARY KEY,
    id_orden           BIGINT NOT NULL,
    id_lote_producto   BIGINT NOT NULL,
    cantidad_obtenida  NUMERIC(14,3) NOT NULL,
    cantidad_rechazada NUMERIC(14,3) NOT NULL DEFAULT 0,
    observaciones      VARCHAR(500),
    CONSTRAINT fk_resultados_orden
        FOREIGN KEY (id_orden)
        REFERENCES produccion.ordenes_produccion (id_orden)
        ON DELETE CASCADE,
    CONSTRAINT fk_resultados_lote_producto
        FOREIGN KEY (id_lote_producto)
        REFERENCES inventario.lotes (id_lote),
    CONSTRAINT uq_resultados_orden_lote
        UNIQUE (id_orden, id_lote_producto),
    CONSTRAINT ck_resultados_cantidades
        CHECK (cantidad_obtenida >= 0 AND cantidad_rechazada >= 0)
);

-- =========================================================
-- COMPRAS
-- =========================================================

CREATE TABLE compras.proveedores (
    id_proveedor BIGSERIAL PRIMARY KEY,
    documento    VARCHAR(30) NOT NULL UNIQUE,
    razon_social VARCHAR(200) NOT NULL,
    telefono     VARCHAR(30),
    correo       VARCHAR(254),
    activo       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE compras.compras (
    id_compra     BIGSERIAL PRIMARY KEY,
    id_proveedor  BIGINT NOT NULL,
    id_usuario    BIGINT NOT NULL,
    numero_compra VARCHAR(50) NOT NULL UNIQUE,
    fecha_compra  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal      NUMERIC(14,2) NOT NULL,
    total         NUMERIC(14,2) NOT NULL,
    estado        VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    CONSTRAINT fk_compras_proveedor
        FOREIGN KEY (id_proveedor)
        REFERENCES compras.proveedores (id_proveedor),
    CONSTRAINT fk_compras_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES seguridad.usuarios (id_usuario),
    CONSTRAINT ck_compras_importes
        CHECK (subtotal >= 0 AND total >= 0)
);

CREATE TABLE compras.detalle_compra (
    id_detalle     BIGSERIAL PRIMARY KEY,
    id_compra      BIGINT NOT NULL,
    id_lote        BIGINT NOT NULL,
    cantidad       NUMERIC(14,3) NOT NULL,
    costo_unitario NUMERIC(14,4) NOT NULL,
    descuento      NUMERIC(14,2) NOT NULL DEFAULT 0,
    subtotal       NUMERIC(14,2) NOT NULL,
    CONSTRAINT fk_detalle_compra_compra
        FOREIGN KEY (id_compra)
        REFERENCES compras.compras (id_compra)
        ON DELETE CASCADE,
    CONSTRAINT fk_detalle_compra_lote
        FOREIGN KEY (id_lote)
        REFERENCES inventario.lotes (id_lote),
    CONSTRAINT uq_detalle_compra_lote
        UNIQUE (id_compra, id_lote),
    CONSTRAINT ck_detalle_compra_valores
        CHECK (cantidad > 0
               AND costo_unitario >= 0
               AND descuento >= 0
               AND subtotal >= 0)
);

-- =========================================================
-- COMERCIAL
-- =========================================================

CREATE TABLE comercial.clientes (
    id_cliente BIGSERIAL PRIMARY KEY,
    documento  VARCHAR(30) NOT NULL UNIQUE,
    nombres    VARCHAR(200) NOT NULL,
    telefono   VARCHAR(30),
    correo     VARCHAR(254),
    activo     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE comercial.ventas (
    id_venta     BIGSERIAL PRIMARY KEY,
    id_cliente   BIGINT,
    id_usuario   BIGINT NOT NULL,
    numero_venta VARCHAR(50) NOT NULL UNIQUE,
    fecha_venta  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal     NUMERIC(14,2) NOT NULL,
    descuento    NUMERIC(14,2) NOT NULL DEFAULT 0,
    impuesto     NUMERIC(14,2) NOT NULL DEFAULT 0,
    total        NUMERIC(14,2) NOT NULL,
    metodo_pago  VARCHAR(50) NOT NULL,
    estado       VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    CONSTRAINT fk_ventas_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES comercial.clientes (id_cliente),
    CONSTRAINT fk_ventas_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES seguridad.usuarios (id_usuario),
    CONSTRAINT ck_ventas_importes
        CHECK (subtotal >= 0
               AND descuento >= 0
               AND impuesto >= 0
               AND total >= 0)
);

CREATE TABLE comercial.detalle_venta (
    id_detalle      BIGSERIAL PRIMARY KEY,
    id_venta        BIGINT NOT NULL,
    id_lote         BIGINT NOT NULL,
    cantidad        NUMERIC(14,3) NOT NULL,
    precio_unitario NUMERIC(14,2) NOT NULL,
    descuento       NUMERIC(14,2) NOT NULL DEFAULT 0,
    subtotal        NUMERIC(14,2) NOT NULL,
    CONSTRAINT fk_detalle_venta_venta
        FOREIGN KEY (id_venta)
        REFERENCES comercial.ventas (id_venta)
        ON DELETE CASCADE,
    CONSTRAINT fk_detalle_venta_lote
        FOREIGN KEY (id_lote)
        REFERENCES inventario.lotes (id_lote),
    CONSTRAINT ck_detalle_venta_valores
        CHECK (cantidad > 0
               AND precio_unitario >= 0
               AND descuento >= 0
               AND subtotal >= 0)
);

-- Indices para las claves foraneas y consultas habituales.
CREATE INDEX idx_lotes_id_producto
    ON inventario.lotes (id_producto);
CREATE INDEX idx_lotes_fecha_vencimiento
    ON inventario.lotes (fecha_vencimiento);
CREATE INDEX idx_movimientos_id_lote
    ON inventario.movimientos_inventario (id_lote);
CREATE INDEX idx_movimientos_id_usuario
    ON inventario.movimientos_inventario (id_usuario);
CREATE INDEX idx_movimientos_fecha
    ON inventario.movimientos_inventario (fecha_movimiento);
CREATE INDEX idx_formulas_producto_terminado
    ON produccion.formulas (id_producto_terminado);
CREATE INDEX idx_detalle_formula_id_insumo
    ON produccion.detalle_formula (id_insumo);
CREATE INDEX idx_ordenes_id_formula
    ON produccion.ordenes_produccion (id_formula);
CREATE INDEX idx_ordenes_id_responsable
    ON produccion.ordenes_produccion (id_responsable);
CREATE INDEX idx_consumos_id_orden
    ON produccion.consumos_produccion (id_orden);
CREATE INDEX idx_consumos_id_lote
    ON produccion.consumos_produccion (id_lote);
CREATE INDEX idx_resultados_id_lote_producto
    ON produccion.resultados_produccion (id_lote_producto);
CREATE INDEX idx_compras_id_proveedor
    ON compras.compras (id_proveedor);
CREATE INDEX idx_compras_id_usuario
    ON compras.compras (id_usuario);
CREATE INDEX idx_detalle_compra_id_lote
    ON compras.detalle_compra (id_lote);
CREATE INDEX idx_ventas_id_cliente
    ON comercial.ventas (id_cliente);
CREATE INDEX idx_ventas_id_usuario
    ON comercial.ventas (id_usuario);
CREATE INDEX idx_detalle_venta_id_venta
    ON comercial.detalle_venta (id_venta);
CREATE INDEX idx_detalle_venta_id_lote
    ON comercial.detalle_venta (id_lote);

COMMIT;
