package com.proyecto.servicios.service;

import com.proyecto.servicios.model.pagos.PagoRequest;
import com.proyecto.servicios.model.pagos.PagoResponse;

public interface PagoService {

    PagoResponse pagar(Integer clienteId, String claveIdempotencia, PagoRequest request);

    PagoResponse consultar(Integer clienteId, Long idPago);

    int conciliarPendientes();
}
