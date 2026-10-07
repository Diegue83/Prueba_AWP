package com.proyecto.servicios.enums;

/**
 * Estatus de la cuenta bancaria.
 * En la base de datos solo se guarda el código de un carácter.
 */
public enum EstatusCuenta implements Catalogo {
    ACTIVA("A"),
    INACTIVA("I");

    private final String codigo;

    EstatusCuenta(String codigo) {
        this.codigo = codigo;
    }

    @Override
    public String getCodigo() {
        return codigo;
    }
}
