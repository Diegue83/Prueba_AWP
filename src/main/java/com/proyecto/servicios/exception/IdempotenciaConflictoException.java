package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class IdempotenciaConflictoException extends NegocioException {

    public IdempotenciaConflictoException() {
        super(HttpStatus.CONFLICT, "IDEMPOTENCIA_CONFLICTO", "La clave de idempotencia ya se usó con datos distintos; genera una nueva para otra operación");
    }
}
