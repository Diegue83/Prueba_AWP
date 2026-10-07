package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.NacionalidadResponse;
import java.util.List;

public interface NacionalidadService {

    List<NacionalidadResponse> consultarTodas();

    /** Para el endpoint de consulta: 404 si no existe. */
    NacionalidadResponse consultar(String valor);

    /** Para el alta y la actualización de clientes: 400 si no existe. Devuelve el nombre tal como está en el catálogo. */
    String obtenerNombreValido(String valor);
}