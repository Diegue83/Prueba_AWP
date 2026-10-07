package com.proyecto.servicios.model.onboarding;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LoginResponse {
    private String mensaje;
    private Integer clienteId;
    private String nombreCompleto;
    private String correo;
    private String numeroCuenta;
    private LocalDate ultimoAcceso;
}
