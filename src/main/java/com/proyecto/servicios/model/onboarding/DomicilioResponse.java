package com.proyecto.servicios.model.onboarding;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DomicilioResponse {
    private String calle;
    private String numeroExterior;
    private String numeroInterior;
    private String colonia;
    private String municipio;
    private String estado;
    private String codigoPostal;
    private String pais;
}
