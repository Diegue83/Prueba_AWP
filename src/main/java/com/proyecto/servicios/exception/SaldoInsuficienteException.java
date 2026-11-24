package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class SaldoInsuficienteException extends NegocioException {

    public SaldoInsuficienteException() {
        super(HttpStatus.CONFLICT, "SALDO_INSUFICIENTE", "El saldo disponible no es suficiente para realizar el pago");
    }
}
