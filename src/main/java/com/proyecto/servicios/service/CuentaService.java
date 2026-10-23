package com.proyecto.servicios.service;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.onboarding.CuentaRequest;
import com.proyecto.servicios.model.onboarding.ConsultaCuentaRequest;

public interface CuentaService {

    GenericResponse consultarPorNumero(CuentaRequest request);

    GenericResponse consultar(ConsultaCuentaRequest request);

    GenericResponse consultarActivas();

    GenericResponse consultarSaldo(CuentaRequest request);
}
