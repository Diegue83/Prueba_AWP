package com.proyecto.servicios.client;

import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.exception.GestoPagoComunicacionException;
import com.proyecto.servicios.exception.GestoPagoIntegracionException;
import com.proyecto.servicios.exception.GestoPagoRespuestaException;
import com.proyecto.servicios.exception.GestoPagoTimeoutException;
import feign.FeignException;
import feign.RetryableException;
import org.springframework.http.HttpStatus;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

public final class GestoPagoErrores {

    private GestoPagoErrores() {
    }

    public static GestoPagoIntegracionException traducir(FeignException e, String operacion) {
        if (e instanceof RetryableException || e.status() < 0) {
            return esTimeout(e)
                    ? new GestoPagoTimeoutException(operacion)
                    : new GestoPagoComunicacionException(operacion);
        }
        if (e.status() == HttpStatus.UNAUTHORIZED.value() || e.status() == HttpStatus.FORBIDDEN.value()) {
            return new GestoPagoAutenticacionException(operacion);
        }
        if (e.status() == HttpStatus.GATEWAY_TIMEOUT.value() || e.status() == HttpStatus.REQUEST_TIMEOUT.value()) {
            return new GestoPagoTimeoutException(operacion);
        }
        return new GestoPagoRespuestaException(operacion, "HTTP " + e.status());
    }

    private static boolean esTimeout(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof SocketTimeoutException || causa instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
