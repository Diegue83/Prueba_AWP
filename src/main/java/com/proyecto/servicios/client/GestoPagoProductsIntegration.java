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
    private final GestoPagoBitacoraService bitacora;

    public GestoPagoProductsIntegration(GestoPagoProductsClient productsClient, GestoPagoBitacoraService bitacora) {
        this.productsClient = productsClient;
        this.bitacora = bitacora;
    }

    public String consultarCatalogoXml(String token) {
        log.info("Inicio de invocación a GestoPago {}", OPERACION);
        long inicio = System.currentTimeMillis();
        try {
            String xml = productsClient.getProductList(BEARER + token);
            bitacora.registrar(OPERACION, null, null, 200, xml, System.currentTimeMillis() - inicio);
            log.info("Fin de invocación a GestoPago {}: exitosa en {} ms ({} caracteres)",
                    OPERACION, System.currentTimeMillis() - inicio, xml == null ? 0 : xml.length());
            return xml;
        } catch (FeignException e) {
            bitacora.registrar(OPERACION, null, null, e.status() < 0 ? null : e.status(),
                    e.status() < 0 ? e.getClass().getSimpleName() : e.contentUTF8(), System.currentTimeMillis() - inicio);
            log.error("Fin de invocación a GestoPago {}: falló en {} ms con HTTP {} ({})",
                    OPERACION, System.currentTimeMillis() - inicio, e.status(), e.getClass().getSimpleName());
            throw GestoPagoErrores.traducir(e, OPERACION);
        }
    }
}
