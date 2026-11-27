package com.proyecto.servicios.service;

import com.proyecto.servicios.model.pagos.ReferenciaRequest;
import com.proyecto.servicios.model.pagos.ReferenciaResponse;

public interface ReferenciaService {

    ReferenciaResponse verificar(ReferenciaRequest request);
}
