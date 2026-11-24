-- =====================================================================
-- V6: pagos de servicios/recargas con GestoPago (sendTx) y segundo factor
--
--   * segundo_factor: secreto TOTP del cliente (cifrado con AES-GCM por la
--     aplicación), último paso de tiempo aceptado para que un código no se
--     use dos veces, y contador de intentos fallidos para bloquear fuerza bruta.
--   * pagos: cada operación nace PENDIENTE con el saldo ya retenido. El upc
--     identifica la transacción ante GestoPago y es único; la pareja
--     (cliente_id, clave_idempotencia) impide procesar dos veces la misma
--     solicitud del cliente.
-- =====================================================================

CREATE TABLE IF NOT EXISTS segundo_factor (
    cliente_id         INTEGER    NOT NULL,
    activo             BOOLEAN    NOT NULL DEFAULT FALSE,
    ultimo_paso        BIGINT,
    intentos_fallidos  SMALLINT   NOT NULL DEFAULT 0,
    bloqueado_hasta    TIMESTAMP,
    fecha_creacion     TIMESTAMP  NOT NULL DEFAULT NOW(),
    fecha_activacion   TIMESTAMP,
    secreto_cifrado    TEXT       NOT NULL,

    CONSTRAINT pk_segundo_factor          PRIMARY KEY (cliente_id),
    CONSTRAINT fk_segundo_factor_cliente  FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_segundo_factor_intentos CHECK (intentos_fallidos >= 0)
);

CREATE TABLE IF NOT EXISTS pagos (
    id                     BIGINT         GENERATED ALWAYS AS IDENTITY,
    cliente_id             INTEGER        NOT NULL,
    cuenta_id              INTEGER        NOT NULL,
    id_producto            INTEGER        NOT NULL,
    id_servicio            INTEGER        NOT NULL,
    monto                  NUMERIC(12,2)  NOT NULL,
    intentos_conciliacion  SMALLINT       NOT NULL DEFAULT 0,
    fecha_creacion         TIMESTAMP      NOT NULL DEFAULT NOW(),
    fecha_actualizacion    TIMESTAMP      NOT NULL DEFAULT NOW(),
    estatus                TEXT           NOT NULL,
    clave_idempotencia     TEXT           NOT NULL,
    upc                    TEXT           NOT NULL,
    hora_local             TEXT           NOT NULL,
    telefono               TEXT,
    referencia             TEXT,
    codigo_respuesta       TEXT,
    mensaje_respuesta      TEXT,
    num_autorizacion       TEXT,
    id_tx                  TEXT,

    CONSTRAINT pk_pagos                PRIMARY KEY (id),
    CONSTRAINT fk_pagos_cliente        FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_pagos_cuenta         FOREIGN KEY (cuenta_id) REFERENCES cuentas (id),
    CONSTRAINT fk_pagos_producto       FOREIGN KEY (id_producto) REFERENCES gestopago_productos (id_producto),
    CONSTRAINT uq_pagos_upc            UNIQUE (upc),
    CONSTRAINT uq_pagos_idempotencia   UNIQUE (cliente_id, clave_idempotencia),
    CONSTRAINT ck_pagos_monto          CHECK (monto > 0),
    CONSTRAINT ck_pagos_estatus        CHECK (estatus IN ('PENDIENTE', 'APLICADO', 'RECHAZADO',
                                                          'EN_CONCILIACION', 'REVISION_MANUAL')),
    CONSTRAINT ck_pagos_clave          CHECK (clave_idempotencia ~ '^[A-Za-z0-9_-]{8,64}$')
);

CREATE INDEX IF NOT EXISTS idx_pagos_por_conciliar ON pagos (fecha_creacion)
    WHERE estatus IN ('PENDIENTE', 'EN_CONCILIACION');
CREATE INDEX IF NOT EXISTS idx_pagos_cuenta ON pagos (cuenta_id);
