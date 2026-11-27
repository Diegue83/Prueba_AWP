package com.proyecto.servicios.model.gestopago;

import java.math.BigDecimal;

/**
 * Respuesta de verifyReference. saldo positivo es un adeudo, cero significa sin adeudo y
 * negativo un saldo a favor; puede venir vacío según el servicio.
 */
public record GestoPagoReferenciaResultado(GestoPagoTxResultado resultado, BigDecimal saldo) {
}
