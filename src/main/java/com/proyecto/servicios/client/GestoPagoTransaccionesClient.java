package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "gestoPagoTransacciones", url = "${gestopago.transacciones.url}")
public interface GestoPagoTransaccionesClient {

    @PostMapping(value = "/sistema/service/sendTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    String sendTx(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                  @RequestBody Map<String, ?> formulario);

    @PostMapping(value = "/sistema/service/confirmTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    String confirmTx(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                     @RequestBody Map<String, ?> formulario);
}
