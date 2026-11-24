package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.EstatusCuenta;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ConsultaCuentaRequest extends SolicitudEstricta {

    @Pattern(regexp = "^\\d{10}$", message = "numeroCuenta: debe contener exactamente 10 dígitos")
    private String numeroCuenta;

    private EstatusCuenta estatus;

    private LocalDate fechaAperturaDesde;

    private LocalDate fechaAperturaHasta;
}
