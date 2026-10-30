package com.proyecto.servicios.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Parámetros de sendTx/confirmTx (prefijo gestopago.transacciones). La llave y el IV AES
 * los asigna el proveedor y solo llegan por variables de entorno; sin ellos los pagos
 * responden 503 y la aplicación sigue funcionando para lo demás.
 */
@Component
@ConfigurationProperties(prefix = "gestopago.transacciones")
@Getter
@Setter
public class GestoPagoTransaccionesProperties {

    private String llaveAes = "";

    private String ivAes = "";

    private String unidad = "";

    /**
     * Códigos con los que el proveedor no confirma ni descarta la operación
     * (por ejemplo 03, "el carrier no responde"): se consultan con confirmTx.
     */
    private Set<String> codigosIndeterminados = Set.of("03");

    /**
     * Códigos de confirmTx que garantizan que la operación NO se aplicó y permiten reembolsar.
     * Vacío por defecto: mientras el proveedor no documente esos códigos, un "no" de
     * confirmTx deja el pago en conciliación en lugar de devolver dinero que pudo cobrarse.
     */
    private Set<String> codigosNoAplicada = Set.of();
}
