package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class GestoPagoAutenticacionException extends GestoPagoIntegracionException {

    public GestoPagoAutenticacionException(String operacion) {
        super(HttpStatus.BAD_GATEWAY, "ERROR_AUTENTICACION_GESTOPAGO",
                "GestoPago rechazó las credenciales o el token de la aplicación (" + operacion + ")");
    }
}
