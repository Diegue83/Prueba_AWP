package com.proyecto.servicios.model.pagos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * VALIDA: el proveedor confirmó la referencia. INVALIDA: la rechazó. NO_VERIFICADA: no respondió
 * de forma concluyente; en ese caso no se debe continuar con el pago.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReferenciaResponse(
        Estado estado,
        boolean valida,
        Integer idServicio,
        Integer idProducto,
        String servicio,
        String producto,
        String referencia,
        String codigoProveedor,
        String mensaje,
        BigDecimal saldo) {

    public enum Estado {
        VALIDA,
        INVALIDA,
        NO_VERIFICADA
    }
}
