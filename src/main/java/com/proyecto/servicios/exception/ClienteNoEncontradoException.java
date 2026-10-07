package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ClienteNoEncontradoException extends NegocioException {

    public ClienteNoEncontradoException(String criterio) {
        super(HttpStatus.BAD_REQUEST, "CLIENTE_NO_ENCONTRADO", "No existe un cliente con " + criterio);
    }
}
