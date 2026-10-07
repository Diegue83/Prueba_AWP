package com.proyecto.servicios.model.onboarding;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SaldoResponse {
    private String numeroCuenta;
    private BigDecimal saldo;
}
