package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Lo que el manejador global necesita de cualquier error de negocio para armar la respuesta.
 */
public interface ErrorNegocio {

    HttpStatus getEstatus();

    String getClave();

    String getMessage();

    List<String> getDetalles();
}
