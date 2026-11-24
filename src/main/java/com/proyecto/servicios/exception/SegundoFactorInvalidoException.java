package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class SegundoFactorInvalidoException extends NegocioException {

    public SegundoFactorInvalidoException() {
        super(HttpStatus.UNAUTHORIZED, "CODIGO_2FA_INVALIDO",
                "El código de verificación no es válido o ya fue utilizado");
    }
}
