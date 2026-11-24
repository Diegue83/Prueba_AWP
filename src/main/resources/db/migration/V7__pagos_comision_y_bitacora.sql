-- =====================================================================
-- V7: comisión de pagos de servicio y bitácora de GestoPago
--
--   * En los pagos de servicio (tipoFront 2) el "precio" del catálogo es la
--     comisión; se retiene monto + comisión y se guardan por separado.
--   * La especificación de PuntoRed obliga a conservar las peticiones y
--     respuestas intercambiadas con el proveedor para aclaraciones.
-- =====================================================================

ALTER TABLE pagos ADD COLUMN IF NOT EXISTS tipo_front INTEGER NOT NULL DEFAULT 1;
ALTER TABLE pagos ADD COLUMN IF NOT EXISTS comision NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE pagos ADD CONSTRAINT ck_pagos_comision CHECK (comision >= 0);

CREATE TABLE IF NOT EXISTS gestopago_bitacora (
    id            BIGINT     GENERATED ALWAYS AS IDENTITY,
    fecha         TIMESTAMP  NOT NULL DEFAULT NOW(),
    duracion_ms   INTEGER    NOT NULL,
    http_status   INTEGER,
    operacion     TEXT       NOT NULL,
    upc           TEXT,
    peticion      TEXT,
    respuesta     TEXT,

    CONSTRAINT pk_gestopago_bitacora PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_gestopago_bitacora_upc ON gestopago_bitacora (upc);
CREATE INDEX IF NOT EXISTS idx_gestopago_bitacora_fecha ON gestopago_bitacora (fecha);
