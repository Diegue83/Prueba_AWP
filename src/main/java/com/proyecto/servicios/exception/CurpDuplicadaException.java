package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CurpDuplicadaException extends NegocioException {

    public CurpDuplicadaException(String curp) {
        super(HttpStatus.BAD_REQUEST, "CURP_DUPLICADA", "Ya existe un cliente con la CURP " + curp);
    }
}
