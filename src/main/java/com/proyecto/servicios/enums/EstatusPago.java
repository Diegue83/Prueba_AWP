package com.proyecto.servicios.enums;

/**
 * Ciclo de vida de un pago:
 * PENDIENTE (saldo retenido, aún sin respuesta del proveedor) termina en APLICADO o RECHAZADO
 * (con reembolso); si no se pudo saber qué pasó, pasa a EN_CONCILIACION y, agotados los
 * intentos de confirmTx, a REVISION_MANUAL con el saldo todavía retenido.
 */
public enum EstatusPago {
    PENDIENTE,
    APLICADO,
    RECHAZADO,
    EN_CONCILIACION,
    REVISION_MANUAL;

    public boolean esFinal() {
        return this == APLICADO || this == RECHAZADO;
    }
}
