package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.EstatusCuenta;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CuentaResponse {
    private String numeroCuenta;
    private Integer clienteId;
    private BigDecimal saldo;
    private EstatusCuenta estatus;
    private LocalDate fechaApertura;
}
