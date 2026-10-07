package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Base de todas las excepciones de negocio. Cada subclase declara su propio
 * código HTTP y su clave de error, así el manejador global necesita UN solo
 * método para todas en lugar de uno por excepción.
 */
@Getter
public abstract class NegocioException extends RuntimeException {

    private final HttpStatus estatus;
    private final String clave;

    private final List<String> detalles;

    protected NegocioException(HttpStatus estatus, String clave, String mensaje) {
        this(estatus, clave, mensaje, List.of());
    }

    protected NegocioException(HttpStatus estatus, String clave, String mensaje, List<String> detalles) {
        super(mensaje);
        this.estatus = estatus;
        this.clave = clave;
        this.detalles = List.copyOf(detalles);
    }
}
