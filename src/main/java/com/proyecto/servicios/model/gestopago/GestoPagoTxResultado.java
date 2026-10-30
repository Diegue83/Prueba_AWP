package com.proyecto.servicios.model.gestopago;

/**
 * Lo que se pudo concluir de una respuesta de sendTx/confirmTx.
 * INDETERMINADO significa "no sabemos si el proveedor cobró": nunca debe tratarse como rechazo.
 */
public record GestoPagoTxResultado(Tipo tipo, String codigo, String mensaje, String numAutorizacion, String idTx) {

    public enum Tipo {
        EXITOSO,
        RECHAZADO,
        INDETERMINADO
    }

    public static GestoPagoTxResultado indeterminado(String motivo) {
        return new GestoPagoTxResultado(Tipo.INDETERMINADO, null, motivo, null, null);
    }

    public boolean esExitoso() {
        return tipo == Tipo.EXITOSO;
    }

    public boolean esRechazado() {
        return tipo == Tipo.RECHAZADO;
    }

    public boolean esIndeterminado() {
        return tipo == Tipo.INDETERMINADO;
    }
}
