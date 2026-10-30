package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;

/**
 * JSON de negocio de sendTx/confirmTx antes de cifrarse. confirmTx debe recibir exactamente
 * los mismos valores (incluidos upc y horaLocal) para que el proveedor ubique la operación.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"idServicio", "idProducto", "referencia", "montoPago", "telefono", "horaLocal", "upc", "unidad"})
public record GestoPagoTxPayload(
        Integer idServicio,
        Integer idProducto,
        String referencia,
        BigDecimal montoPago,
        String telefono,
        String horaLocal,
        String upc,
        String unidad) {
}
