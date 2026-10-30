package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoProducts", url = "${gestopago.productos.url}")
public interface GestoPagoProductsClient {

    @GetMapping(value = "/sistema/service/getProductList.do",
            produces = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    String getProductList(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
}
