package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NacionalidadRequest extends SolicitudEstricta {

    @NotBlank(message = "valor: es obligatorio")
    private String valor;
}
