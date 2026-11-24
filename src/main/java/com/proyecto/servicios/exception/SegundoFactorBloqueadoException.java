package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class SegundoFactorBloqueadoException extends NegocioException {

    public SegundoFactorBloqueadoException() {
        super(HttpStatus.LOCKED, "2FA_BLOQUEADO",
                "Demasiados códigos incorrectos. Espera unos minutos antes de volver a intentarlo");
    }
}
