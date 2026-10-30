package com.proyecto.servicios.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Habla con sendTx.do y confirmTx.do. Traduce cualquier falla de red, timeout o 5xx a un
 * resultado INDETERMINADO: a esas alturas el proveedor pudo haber cobrado y quien llama
 * debe conciliar, nunca reintentar a ciegas ni reembolsar.
 */
@Component
@Slf4j
public class GestoPagoTransaccionesIntegration {

    public static final String SEND_TX = "sendTx";
    public static final String CONFIRM_TX = "confirmTx";
    private static final String BEARER = "Bearer ";

    private final GestoPagoTransaccionesClient client;
    private final GestoPagoCifrador cifrador;
    private final GestoPagoTxXmlParser parser;
    private final ObjectMapper objectMapper;

    public GestoPagoTransaccionesIntegration(GestoPagoTransaccionesClient client,
                                             GestoPagoCifrador cifrador,
                                             GestoPagoTxXmlParser parser,
                                             ObjectMapper objectMapper) {
        this.client = client;
        this.cifrador = cifrador;
        this.parser = parser;
        this.objectMapper = objectMapper;
    }

    public GestoPagoTxResultado enviar(GestoPagoTxPayload payload, String token) {
        return invocar(SEND_TX, payload, token, client::sendTx);
    }

    public GestoPagoTxResultado confirmar(GestoPagoTxPayload payload, String token) {
        return invocar(CONFIRM_TX, payload, token, client::confirmTx);
    }

    private GestoPagoTxResultado invocar(String operacion, GestoPagoTxPayload payload, String token,
                                         BiFunction<String, Map<String, ?>, String> llamada) {
        Map<String, String> formulario = Map.of(
                "random", String.valueOf(System.currentTimeMillis()),
                "signed", cifrador.cifrar(json(payload)));

        log.info("Inicio de invocación a GestoPago {} upc={}", operacion, payload.upc());
        long inicio = System.currentTimeMillis();
        try {
            GestoPagoTxResultado resultado = parser.interpretar(llamada.apply(BEARER + token, formulario));
            log.info("Fin de invocación a GestoPago {} upc={}: {} código={} en {} ms",
                    operacion, payload.upc(), resultado.tipo(), resultado.codigo(), System.currentTimeMillis() - inicio);
            return resultado;
        } catch (FeignException e) {
            long duracion = System.currentTimeMillis() - inicio;
            if (e.status() == HttpStatus.UNAUTHORIZED.value() || e.status() == HttpStatus.FORBIDDEN.value()) {
                log.warn("Fin de invocación a GestoPago {} upc={}: token rechazado (HTTP {}) en {} ms",
                        operacion, payload.upc(), e.status(), duracion);
                throw new GestoPagoAutenticacionException(operacion);
            }
            if (e.status() >= 400 && e.status() < 500) {
                log.error("Fin de invocación a GestoPago {} upc={}: rechazada con HTTP {} en {} ms",
                        operacion, payload.upc(), e.status(), duracion);
                return new GestoPagoTxResultado(GestoPagoTxResultado.Tipo.RECHAZADO, "HTTP_" + e.status(),
                        "El proveedor rechazó la solicitud", null, null);
            }
            log.error("Fin de invocación a GestoPago {} upc={}: sin respuesta concluyente ({}, HTTP {}) en {} ms",
                    operacion, payload.upc(), e.getClass().getSimpleName(), e.status(), duracion);
            return GestoPagoTxResultado.indeterminado("sin respuesta concluyente del proveedor");
        }
    }

    private String json(GestoPagoTxPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible serializar la transacción", e);
        }
    }
}
