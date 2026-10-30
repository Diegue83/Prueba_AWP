package com.proyecto.servicios.model.pagos;

import com.proyecto.servicios.model.onboarding.SolicitudEstricta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CodigoSegundoFactorRequest extends SolicitudEstricta {

    @NotBlank(message = "codigo: es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "codigo: debe contener exactamente 6 dígitos")
    private String codigo;
}
