-- Reemplaza la tabla de V4 (payload JSON sin estructura) por las columnas que entrega
-- getProductList.do: cada <producto> trae sus datos como atributos y la leyenda en CDATA.
DROP TABLE IF EXISTS gestopago_productos;

CREATE TABLE gestopago_productos (
    id_producto             INTEGER        NOT NULL,
    id_servicio             INTEGER        NOT NULL,
    id_cat_tipo_servicio    INTEGER        NOT NULL,
    tipo_front              INTEGER        NOT NULL,
    precio                  NUMERIC(12,2)  NOT NULL,
    has_digito_verificador  BOOLEAN        NOT NULL DEFAULT FALSE,
    show_ayuda              BOOLEAN        NOT NULL DEFAULT FALSE,
    fecha_creacion          TIMESTAMP      NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP      NOT NULL DEFAULT NOW(),
    servicio                TEXT           NOT NULL,
    producto                TEXT           NOT NULL,
    tipo_referencia         TEXT,
    leyenda                 TEXT,

    CONSTRAINT pk_gestopago_productos        PRIMARY KEY (id_producto),
    CONSTRAINT ck_gestopago_productos_precio CHECK (precio >= 0)
);

CREATE INDEX IF NOT EXISTS idx_gestopago_productos_servicio ON gestopago_productos (id_servicio);
