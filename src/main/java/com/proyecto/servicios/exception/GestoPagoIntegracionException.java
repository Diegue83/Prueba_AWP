package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public abstract class GestoPagoIntegracionException extends NegocioException {

    protected GestoPagoIntegracionException(HttpStatus estatus, String clave, String mensaje) {
        super(estatus, clave, mensaje);
    }
}
