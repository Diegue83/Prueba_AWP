package com.proyecto.servicios.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GestoPagoProductsIntegration {

    public static final String OPERACION = "getProductList";
    private static final String BEARER = "Bearer ";

    private final GestoPagoProductsClient productsClient;

    public GestoPagoProductsIntegration(GestoPagoProductsClient productsClient) {
        this.productsClient = productsClient;
    }

    public String consultarCatalogoXml(String token) {
        log.info("Inicio de invocación a GestoPago {}", OPERACION);
        long inicio = System.currentTimeMillis();
        try {
            String xml = productsClient.getProductList(BEARER + token);
            log.info("Fin de invocación a GestoPago {}: exitosa en {} ms ({} caracteres)",
                    OPERACION, System.currentTimeMillis() - inicio, xml == null ? 0 : xml.length());
            return xml;
        } catch (FeignException e) {
            log.error("Fin de invocación a GestoPago {}: falló en {} ms con HTTP {} ({})",
                    OPERACION, System.currentTimeMillis() - inicio, e.status(), e.getClass().getSimpleName());
            throw GestoPagoErrores.traducir(e, OPERACION);
        }
    }
}
