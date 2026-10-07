package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class RfcDuplicadoException extends NegocioException {

    public RfcDuplicadoException(String rfc) {
        super(HttpStatus.CONFLICT, "RFC_DUPLICADO", "Ya existe un cliente con el RFC " + rfc);
    }
}
