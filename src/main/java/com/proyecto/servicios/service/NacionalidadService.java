package com.proyecto.servicios.service;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.onboarding.NacionalidadResponse;
import com.proyecto.servicios.model.onboarding.NacionalidadRequest;

public interface NacionalidadService {

    GenericResponse consultarTodas();

    /** Para el endpoint de consulta: 404 si no existe. */
    GenericResponse consultar(NacionalidadRequest request);

    /** Para el alta y la actualización de clientes: 400 si no existe. Devuelve el nombre tal como está en el catálogo. */
    String obtenerNombreValido(String valor);
}