package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class GestoPagoRespuestaException extends GestoPagoIntegracionException {

    public GestoPagoRespuestaException(String operacion, String motivo) {
        super(HttpStatus.BAD_GATEWAY, "RESPUESTA_NO_EXITOSA_GESTOPAGO",
                "GestoPago respondió sin éxito (" + operacion + "): " + motivo);
    }
}
