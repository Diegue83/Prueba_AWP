package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.proyecto.servicios.exception.ValidacionException;

/**
 * Base de todos los cuerpos de petición. Si el JSON trae un campo que el DTO no
 * declara (por ejemplo "curp" en una actualización), la petición se rechaza con
 * 400 en lugar de ignorar el dato en silencio.
 */
public abstract class SolicitudEstricta {

    /** Jackson llama a este método por cada campo del JSON que no corresponde a un atributo. */
    @JsonAnySetter
    public void rechazarCampoDesconocido(String campo, Object valor) {
        throw new ValidacionException("El campo '" + campo + "' no existe o no se puede enviar en esta operación");
    }
}
