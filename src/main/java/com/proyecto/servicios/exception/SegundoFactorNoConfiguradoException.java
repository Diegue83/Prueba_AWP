package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class SegundoFactorNoConfiguradoException extends NegocioException {

    public SegundoFactorNoConfiguradoException() {
        super(HttpStatus.FORBIDDEN, "2FA_NO_CONFIGURADO",
                "Activa el segundo factor de autenticación antes de realizar pagos");
    }
}
