package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class GestoPagoTimeoutException extends GestoPagoIntegracionException {

    public GestoPagoTimeoutException(String operacion) {
        super(HttpStatus.GATEWAY_TIMEOUT, "TIMEOUT_GESTOPAGO",
                "GestoPago no respondió dentro del tiempo permitido (" + operacion + ")");
    }
}
