package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/** No hay token, es inválido o ya expiró. */
public class NoAutenticadoException extends NegocioException {

    public NoAutenticadoException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", mensaje);
    }
}