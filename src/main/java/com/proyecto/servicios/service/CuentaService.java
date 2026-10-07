package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.model.onboarding.SaldoResponse;

import java.util.List;

public interface CuentaService {

    CuentaResponse consultarPorNumero(String numeroCuenta);

    List<CuentaResponse> consultarActivas();

    SaldoResponse consultarSaldo(String numeroCuenta);
}
