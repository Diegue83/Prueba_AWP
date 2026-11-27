package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public abstract class GestoPagoIntegracionException extends RuntimeException implements ErrorNegocio {

    private final HttpStatus estatus;
    private final String clave;

    protected GestoPagoIntegracionException(HttpStatus estatus, String clave, String mensaje) {
        super(mensaje);
        this.estatus = estatus;
        this.clave = clave;
    }

    @Override
    public List<String> getDetalles() {
        return List.of();
    }
}
