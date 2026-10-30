package com.proyecto.servicios.model.pagos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.enums.EstatusPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagoResponse(
        Long idPago,
        EstatusPago estatus,
        Integer idProducto,
        BigDecimal monto,
        String numAutorizacion,
        String codigoProveedor,
        String mensaje,
        LocalDateTime fecha) {
}
