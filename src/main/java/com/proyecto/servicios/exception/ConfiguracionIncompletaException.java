package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ConfiguracionIncompletaException extends NegocioException {

    public ConfiguracionIncompletaException(String funcionalidad) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_CONFIGURADO",
                "El servicio de " + funcionalidad + " no está disponible por el momento");
    }
}
