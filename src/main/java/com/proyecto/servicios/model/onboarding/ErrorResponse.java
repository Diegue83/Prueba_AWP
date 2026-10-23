package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Respuesta de error. Extiende el GenericResponse del proyecto (codigo + mensaje)
 * y agrega una clave del tipo de error y el detalle campo por campo.
 */
@Getter
@Setter
public class ErrorResponse extends GenericResponse {

    private String error;
    private List<String> detalles;

    public ErrorResponse(String error, String mensaje, List<String> detalles) {
        setCodigo(1);
        setMensaje(mensaje);
        setDatos(detalles);
        this.error = error;
        this.detalles = detalles;
    }
}
