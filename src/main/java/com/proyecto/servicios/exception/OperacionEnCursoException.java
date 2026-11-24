package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class OperacionEnCursoException extends NegocioException {

    public OperacionEnCursoException() {
        super(HttpStatus.CONFLICT, "OPERACION_EN_CURSO", "La cuenta tiene otra operación en curso; intenta de nuevo en unos segundos");
    }
}
