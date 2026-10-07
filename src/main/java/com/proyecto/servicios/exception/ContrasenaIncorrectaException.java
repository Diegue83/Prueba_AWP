package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/** Login: el correo existe pero la contraseña no coincide. */
public class ContrasenaIncorrectaException extends NegocioException {

    public ContrasenaIncorrectaException() {
        super(HttpStatus.UNAUTHORIZED, "CONTRASENA_INCORRECTA", "Contraseña errónea");
    }
}
