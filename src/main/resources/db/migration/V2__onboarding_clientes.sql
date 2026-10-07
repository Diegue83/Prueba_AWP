-- =====================================================================
-- V2: Onboarding de clientes personas fisicas
-- Tablas: clientes, domicilios, cuentas, login
--
-- Criterios de diseno:
--   * Todos los textos son TEXT. En PostgreSQL, TEXT y VARCHAR(n) ocupan lo
--     mismo y rinden igual; el limite de longitud y el formato se garantizan
--     con restricciones CHECK, que ademas dan un nombre claro a cada regla.
--   * Fechas simples (anio-mes-dia): DATE, 4 bytes, sin hora.
--   * Dinero: NUMERIC(p,2), siempre con 2 decimales exactos
--     (FLOAT/DOUBLE redondean centavos).
--   * Catalogos (sexo, estado civil, estatus): un solo caracter (enum en Java),
--     con CHECK que solo admite los codigos validos.
--   * Banderas: BOOLEAN, 1 byte.
--   * Las columnas de ancho fijo van primero y los textos al final, para
--     que PostgreSQL desperdicie menos bytes de relleno por fila.
--   * domicilios y login reutilizan el id del cliente como llave primaria
--     (relacion 1 a 1): se ahorran una columna y un indice cada una.
-- =====================================================================

-- El numero de cuenta lo entrega la base de datos: unico aunque dos altas
-- ocurran al mismo tiempo. Siempre tiene 10 digitos.
CREATE SEQUENCE IF NOT EXISTS seq_numero_cuenta
    AS BIGINT
    START WITH 1000000001
    MINVALUE 1000000000
    MAXVALUE 9999999999
    NO CYCLE;

-- ---------------------------------------------------------------------
-- clientes: datos personales, de contacto y laborales
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clientes (
    id                  INTEGER        GENERATED ALWAYS AS IDENTITY,
    fecha_nacimiento    DATE           NOT NULL,
    fecha_registro      DATE           NOT NULL DEFAULT CURRENT_DATE,
    fecha_actualizacion DATE           NOT NULL DEFAULT CURRENT_DATE,
    ingreso_mensual     NUMERIC(12,2)  NOT NULL,
    activo              BOOLEAN        NOT NULL DEFAULT TRUE,
    sexo                TEXT           NOT NULL,   -- H=HOMBRE M=MUJER X=NO_BINARIO
    estado_civil        TEXT           NOT NULL,   -- S=SOLTERO C=CASADO D=DIVORCIADO V=VIUDO U=UNION_LIBRE
    curp                TEXT           NOT NULL,
    rfc                 TEXT           NOT NULL,
    telefono_movil      TEXT           NOT NULL,
    telefono_alterno    TEXT,
    nombre              TEXT           NOT NULL,
    segundo_nombre      TEXT,
    apellido_paterno    TEXT           NOT NULL,
    apellido_materno    TEXT           NOT NULL,
    nacionalidad        TEXT           NOT NULL,
    correo              TEXT           NOT NULL,
    ocupacion           TEXT           NOT NULL,
    empresa             TEXT           NOT NULL,

    CONSTRAINT pk_clientes            PRIMARY KEY (id),
    CONSTRAINT uq_clientes_curp       UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc        UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo     UNIQUE (correo),
    CONSTRAINT ck_clientes_sexo       CHECK (sexo IN ('H', 'M', 'X')),
    CONSTRAINT ck_clientes_edo_civil  CHECK (estado_civil IN ('S', 'C', 'D', 'V', 'U')),
    CONSTRAINT ck_clientes_ingreso    CHECK (ingreso_mensual > 0),
    CONSTRAINT ck_clientes_curp       CHECK (curp ~ '^[A-Z0-9]{18}$'),
    CONSTRAINT ck_clientes_rfc        CHECK (rfc ~ '^[A-Z0-9&Ñ]{12,13}$'),
    CONSTRAINT ck_clientes_tel_movil  CHECK (telefono_movil ~ '^[0-9]{10}$'),
    CONSTRAINT ck_clientes_tel_alt    CHECK (telefono_alterno IS NULL OR telefono_alterno ~ '^[0-9]{10}$'),
    CONSTRAINT ck_clientes_nombre     CHECK (char_length(nombre) BETWEEN 2 AND 50),
    CONSTRAINT ck_clientes_seg_nombre CHECK (segundo_nombre IS NULL OR char_length(segundo_nombre) BETWEEN 2 AND 50),
    CONSTRAINT ck_clientes_ap_paterno CHECK (char_length(apellido_paterno) BETWEEN 2 AND 50),
    CONSTRAINT ck_clientes_ap_materno CHECK (char_length(apellido_materno) BETWEEN 2 AND 50),
    CONSTRAINT ck_clientes_nacionalid CHECK (char_length(nacionalidad) BETWEEN 2 AND 40),
    CONSTRAINT ck_clientes_correo     CHECK (char_length(correo) <= 100 AND correo = LOWER(correo)),
    CONSTRAINT ck_clientes_ocupacion  CHECK (char_length(ocupacion) BETWEEN 2 AND 60),
    CONSTRAINT ck_clientes_empresa    CHECK (char_length(empresa) BETWEEN 2 AND 80),
    CONSTRAINT ck_clientes_nacimiento CHECK (fecha_nacimiento < fecha_registro)
);

-- CURP, RFC y correo ya tienen indice por su UNIQUE. Solo faltan estos:
CREATE INDEX IF NOT EXISTS idx_clientes_fecha_registro ON clientes (fecha_registro);
-- Indice parcial: solo guarda las filas activas, que son las que se consultan.
CREATE INDEX IF NOT EXISTS idx_clientes_activos ON clientes (id) WHERE activo;

-- ---------------------------------------------------------------------
-- domicilios: 1 a 1 con clientes (misma llave)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS domicilios (
    cliente_id       INTEGER  NOT NULL,
    codigo_postal    TEXT     NOT NULL,
    numero_exterior  TEXT     NOT NULL,
    numero_interior  TEXT,
    calle            TEXT     NOT NULL,
    colonia          TEXT     NOT NULL,
    municipio        TEXT     NOT NULL,
    estado           TEXT     NOT NULL,
    pais             TEXT     NOT NULL,

    CONSTRAINT pk_domicilios          PRIMARY KEY (cliente_id),
    CONSTRAINT fk_domicilios_cliente  FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_domicilios_cp       CHECK (codigo_postal ~ '^[0-9]{5}$'),
    CONSTRAINT ck_domicilios_num_ext  CHECK (char_length(numero_exterior) BETWEEN 1 AND 10),
    CONSTRAINT ck_domicilios_num_int  CHECK (numero_interior IS NULL OR char_length(numero_interior) BETWEEN 1 AND 10),
    CONSTRAINT ck_domicilios_calle    CHECK (char_length(calle) BETWEEN 2 AND 100),
    CONSTRAINT ck_domicilios_colonia  CHECK (char_length(colonia) BETWEEN 2 AND 80),
    CONSTRAINT ck_domicilios_municip  CHECK (char_length(municipio) BETWEEN 2 AND 80),
    CONSTRAINT ck_domicilios_estado   CHECK (char_length(estado) BETWEEN 2 AND 50),
    CONSTRAINT ck_domicilios_pais     CHECK (char_length(pais) BETWEEN 2 AND 50)
);

-- ---------------------------------------------------------------------
-- cuentas: cuenta bancaria del cliente
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cuentas (
    id              INTEGER        GENERATED ALWAYS AS IDENTITY,
    cliente_id      INTEGER        NOT NULL,
    fecha_apertura  DATE           NOT NULL DEFAULT CURRENT_DATE,
    saldo           NUMERIC(15,2)  NOT NULL DEFAULT 0.00,
    estatus         TEXT           NOT NULL DEFAULT 'A', -- A=ACTIVA I=INACTIVA
    numero_cuenta   TEXT           NOT NULL,

    CONSTRAINT pk_cuentas          PRIMARY KEY (id),
    CONSTRAINT fk_cuentas_cliente  FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT uq_cuentas_numero   UNIQUE (numero_cuenta),
    CONSTRAINT uq_cuentas_cliente  UNIQUE (cliente_id),
    CONSTRAINT ck_cuentas_saldo    CHECK (saldo >= 0),
    CONSTRAINT ck_cuentas_estatus  CHECK (estatus IN ('A', 'I')),
    CONSTRAINT ck_cuentas_numero   CHECK (numero_cuenta ~ '^[0-9]{10}$')
);

CREATE INDEX IF NOT EXISTS idx_cuentas_activas ON cuentas (id) WHERE estatus = 'A';

-- ---------------------------------------------------------------------
-- login: credenciales de acceso del cliente (1 a 1, misma llave).
-- El correo con el que se entra vive en clientes (no se duplica aqui).
-- La contrasena nunca se guarda en claro: solo su hash BCrypt (60 caracteres).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS login (
    cliente_id      INTEGER  NOT NULL,
    fecha_creacion  DATE     NOT NULL DEFAULT CURRENT_DATE,
    ultimo_acceso   DATE,
    contrasena      TEXT     NOT NULL,

    CONSTRAINT pk_login          PRIMARY KEY (cliente_id),
    CONSTRAINT fk_login_cliente  FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_login_hash     CHECK (char_length(contrasena) = 60)
);
