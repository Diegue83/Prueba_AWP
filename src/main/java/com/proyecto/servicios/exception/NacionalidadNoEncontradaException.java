package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class NacionalidadNoEncontradaException extends NegocioException {

    public NacionalidadNoEncontradaException(String valor) {
        super(HttpStatus.NOT_FOUND, "NACIONALIDAD_NO_ENCONTRADA",
                "La nacionalidad '" + valor + "' no existe en el catálogo");
    }
}