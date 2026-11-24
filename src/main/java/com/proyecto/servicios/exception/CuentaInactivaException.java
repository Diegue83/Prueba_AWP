package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CuentaInactivaException extends NegocioException {

    public CuentaInactivaException() {
        super(HttpStatus.CONFLICT, "CUENTA_INACTIVA", "La cuenta no está activa");
    }
}
