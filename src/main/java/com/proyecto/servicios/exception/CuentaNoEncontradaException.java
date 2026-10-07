package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CuentaNoEncontradaException extends NegocioException {

    public CuentaNoEncontradaException(String numeroCuenta) {
        super(HttpStatus.BAD_REQUEST, "CUENTA_NO_ENCONTRADA", "No existe la cuenta " + numeroCuenta);
    }
}
