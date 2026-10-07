package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;

import java.util.List;

public interface ClienteService {

    ClienteResponse registrar(ClienteRequest request);

    ClienteResponse actualizar(Integer id, ClienteUpdateRequest request);

    void darDeBaja(Integer id);

    List<ClienteResponse> consultarTodos();

    List<ClienteResponse> consultarActivos();

    List<ClienteResponse> consultarRegistradosEntre(String desde, String hasta);

    ClienteResponse consultarPorId(Integer id);

    ClienteResponse consultarPorCurp(String curp);

    ClienteResponse consultarPorRfc(String rfc);

    ClienteResponse consultarPorCorreo(String correo);

    ClienteResponse consultarPorNumeroCuenta(String numeroCuenta);
}
