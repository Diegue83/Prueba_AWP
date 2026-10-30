package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class PagoNoEncontradoException extends NegocioException {

    public PagoNoEncontradoException(Long idPago) {
        super(HttpStatus.NOT_FOUND, "PAGO_NO_ENCONTRADO", "No existe el pago " + idPago);
    }
}
