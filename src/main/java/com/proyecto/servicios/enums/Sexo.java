package com.proyecto.servicios.enums;

/**
 * Catálogo de sexo.
 * En la base de datos solo se guarda el código de un carácter.
 */
public enum Sexo implements Catalogo {
    HOMBRE("H"),
    MUJER("M"),
    NO_BINARIO("X");

    private final String codigo;

    Sexo(String codigo) {
        this.codigo = codigo;
    }

    @Override
    public String getCodigo() {
        return codigo;
    }
}
