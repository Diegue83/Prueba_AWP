package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/** El token es válido, pero el recurso pertenece a otro cliente. */
public class AccesoDenegadoException extends NegocioException {

  public AccesoDenegadoException() {
    super(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "No tienes autorización para acceder a este recurso");
  }
}