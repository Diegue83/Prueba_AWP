package com.proyecto.servicios.service;

import com.proyecto.servicios.model.pagos.SegundoFactorRegistroResponse;

public interface SegundoFactorService {

    SegundoFactorRegistroResponse registrar(Integer clienteId);

    void activar(Integer clienteId, String codigo);

    void verificar(Integer clienteId, String codigo);
}
