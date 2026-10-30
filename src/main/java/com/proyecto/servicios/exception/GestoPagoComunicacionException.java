package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class GestoPagoComunicacionException extends GestoPagoIntegracionException {

    public GestoPagoComunicacionException(String operacion) {
        super(HttpStatus.BAD_GATEWAY, "ERROR_COMUNICACION_GESTOPAGO",
                "No fue posible comunicarse con GestoPago (" + operacion + ")");
    }
}
