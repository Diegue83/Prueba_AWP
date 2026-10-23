package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CuentaRequest extends SolicitudEstricta {

    @NotBlank(message = "numeroCuenta: es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "numeroCuenta: debe contener exactamente 10 dígitos")
    private String numeroCuenta;
}
