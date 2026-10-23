package com.proyecto.servicios.service;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ConsultaClienteRequest;

import java.util.List;

public interface ClienteService {

    GenericResponse registrar(ClienteRequest request);

    GenericResponse actualizar(ClienteRequest request);

    GenericResponse darDeBaja(ClienteRequest request);

    GenericResponse consultarTodos();

    GenericResponse consultar(ClienteRequest criterios);

    GenericResponse consultar(ConsultaClienteRequest criterios);

    GenericResponse consultarActivos();

    GenericResponse consultarRegistradosEntre(String desde, String hasta);

    GenericResponse consultarPorId(Integer id);

    GenericResponse consultarPorCurp(String curp);

    GenericResponse consultarPorRfc(String rfc);

    GenericResponse consultarPorCorreo(String correo);

    GenericResponse consultarPorNumeroCuenta(String numeroCuenta);
}
