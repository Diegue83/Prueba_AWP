package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/** Login: no hay ningún cliente registrado con ese correo. */
public class CorreoNoEncontradoException extends NegocioException {

    public CorreoNoEncontradoException(String correo) {
        super(HttpStatus.BAD_REQUEST, "CORREO_NO_ENCONTRADO", "Correo no encontrado: " + correo);
    }
}
