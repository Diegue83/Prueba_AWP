package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ClienteYaRegistradoException extends NegocioException {

    public ClienteYaRegistradoException(String motivo) {
        super(HttpStatus.BAD_REQUEST, "CLIENTE_YA_REGISTRADO", "El cliente ya está registrado: " + motivo);
    }
}
