package com.proyecto.servicios.enums;

/**
 * Catálogo de estado civil.
 * En la base de datos solo se guarda el código de un carácter.
 */
public enum EstadoCivil implements Catalogo {
    SOLTERO("S"),
    CASADO("C"),
    DIVORCIADO("D"),
    VIUDO("V"),
    UNION_LIBRE("U");

    private final String codigo;

    EstadoCivil(String codigo) {
        this.codigo = codigo;
    }

    @Override
    public String getCodigo() {
        return codigo;
    }
}
