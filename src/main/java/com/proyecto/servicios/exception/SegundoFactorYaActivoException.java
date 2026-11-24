package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class SegundoFactorYaActivoException extends NegocioException {

    public SegundoFactorYaActivoException() {
        super(HttpStatus.CONFLICT, "2FA_YA_ACTIVO", "El segundo factor de autenticación ya está activo");
    }
}
