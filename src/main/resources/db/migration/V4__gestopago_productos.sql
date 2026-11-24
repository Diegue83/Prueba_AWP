CREATE TABLE IF NOT EXISTS gestopago_productos (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payload               TEXT         NOT NULL,
    fecha_sincronizacion  TIMESTAMP    NOT NULL,
    CONSTRAINT ck_gestopago_productos_payload_json CHECK (payload::jsonb IS NOT NULL)
);
