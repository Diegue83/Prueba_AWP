package com.proyecto.servicios.model.pagos;

import java.math.BigDecimal;

/**
 * Resultado de validar una solicitud contra el catálogo: lo que se retiene de la cuenta y los
 * valores exactos que viajarán a sendTx (y después, iguales, a confirmTx).
 */
public record PagoValidado(
        Integer idProducto,
        Integer idServicio,
        Integer tipoFront,
        BigDecimal monto,
        BigDecimal comision,
        String telefono,
        String referencia) {

    public BigDecimal total() {
        return monto.add(comision);
    }
}
