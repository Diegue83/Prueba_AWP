package com.proyecto.servicios.model.onboarding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Solo transporta datos: las reglas están en validation/ClienteValidador. */
@Getter
@Setter
@NoArgsConstructor
public class DomicilioRequest extends SolicitudEstricta {
    private String calle;
    private String numeroExterior;
    private String numeroInterior;
    private String colonia;
    private String municipio;
    private String estado;
    private String codigoPostal;
    private String pais;
}
