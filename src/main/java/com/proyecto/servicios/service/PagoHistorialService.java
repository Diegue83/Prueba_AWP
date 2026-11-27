package com.proyecto.servicios.service;

import com.proyecto.servicios.model.pagos.PagoResponse;

import java.util.List;

public interface PagoHistorialService {

    List<PagoResponse> consultarHistorial(Integer clienteId, int pagina, int tamano);

    PagoResponse consultarDetalle(Integer clienteId, Long idPago);
}
