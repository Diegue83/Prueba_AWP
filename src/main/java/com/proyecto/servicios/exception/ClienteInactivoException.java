package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ClienteInactivoException extends NegocioException {

    public ClienteInactivoException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, "CLIENTE_INACTIVO", mensaje);
    }
}
