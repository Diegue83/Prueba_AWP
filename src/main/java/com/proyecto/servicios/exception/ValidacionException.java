package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Datos inválidos enviados por el usuario: siempre responde 400 (Bad Request).
 * Puede llevar la lista con el mensaje de cada campo incorrecto.
 */
public class ValidacionException extends NegocioException {

    public ValidacionException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, "ERROR_VALIDACION", mensaje);
    }

    public ValidacionException(String mensaje, List<String> detalles) {
        super(HttpStatus.BAD_REQUEST, "ERROR_VALIDACION", mensaje, detalles);
    }
}
