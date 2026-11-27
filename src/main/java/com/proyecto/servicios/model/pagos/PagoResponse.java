package com.proyecto.servicios.model.pagos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.entity.pagos.Pago;
import com.proyecto.servicios.enums.EstatusPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * servicio y producto solo se llenan en el historial, donde se cruzan con el catálogo.
 * referencia es el dato que capturó el cliente: la referencia del recibo o, en recargas, el teléfono.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagoResponse(
        Long idPago,
        EstatusPago estatus,
        Integer idServicio,
        Integer idProducto,
        String servicio,
        String producto,
        String referencia,
        BigDecimal monto,
        BigDecimal comision,
        BigDecimal total,
        String numAutorizacion,
        String codigoProveedor,
        String mensaje,
        LocalDateTime fechaOperacion,
        LocalDateTime fecha) {

    public static PagoResponse de(Pago pago, GestoPagoProduct producto) {
        return new PagoResponse(
                pago.getId(),
                pago.getEstatus(),
                pago.getIdServicio(),
                pago.getIdProducto(),
                producto == null ? null : producto.getServicio(),
                producto == null ? null : producto.getProducto(),
                pago.getReferencia() != null ? pago.getReferencia() : pago.getTelefono(),
                pago.getMonto(),
                pago.getComision(),
                pago.total(),
                pago.getNumAutorizacion(),
                pago.getCodigoRespuesta(),
                mensajePara(pago.getEstatus()),
                pago.getFechaCreacion(),
                pago.getFechaActualizacion());
    }

    public static String mensajePara(EstatusPago estatus) {
        return switch (estatus) {
            case APLICADO -> "Pago realizado correctamente";
            case RECHAZADO -> "El proveedor rechazó el pago; el monto se devolvió a tu cuenta";
            case PENDIENTE, EN_CONCILIACION ->
                    "El pago está en verificación con el proveedor; el monto queda retenido hasta confirmarlo";
            case REVISION_MANUAL ->
                    "El pago requiere revisión; el monto queda retenido. Contacta a soporte con el número de pago";
        };
    }
}
