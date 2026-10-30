package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoTransaccionesProperties;
import com.proyecto.servicios.exception.ConfiguracionIncompletaException;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Set;

/**
 * Cifra el JSON de sendTx/confirmTx con AES/CBC/PKCS5Padding y la llave/IV fijos que asigna
 * GestoPago; el resultado en Base64 viaja en el campo "signed". El IV fijo es parte del
 * protocolo del proveedor, no una elección de este proyecto.
 */
@Component
public class GestoPagoCifrador {

    private static final String ALGORITMO = "AES/CBC/PKCS5Padding";
    private static final Set<Integer> TAMANOS_LLAVE = Set.of(16, 24, 32);

    private final GestoPagoTransaccionesProperties properties;

    public GestoPagoCifrador(GestoPagoTransaccionesProperties properties) {
        this.properties = properties;
    }

    /** Se llama antes de retener saldo: sin llave válida no tiene sentido apartar dinero. */
    public void validarConfiguracion() {
        llave();
        iv();
    }

    public String cifrar(String json) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, llave(), iv());
            return Base64.getEncoder().encodeToString(cipher.doFinal(json.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No fue posible cifrar la petición para GestoPago", e);
        }
    }

    public String descifrar(String base64) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, llave(), iv());
            return new String(cipher.doFinal(Base64.getDecoder().decode(base64)), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No fue posible descifrar el contenido de GestoPago", e);
        }
    }

    private SecretKeySpec llave() {
        byte[] llave = decodificar(properties.getLlaveAes());
        if (!TAMANOS_LLAVE.contains(llave.length)) {
            throw new ConfiguracionIncompletaException("pagos");
        }
        return new SecretKeySpec(llave, "AES");
    }

    private IvParameterSpec iv() {
        byte[] iv = decodificar(properties.getIvAes());
        if (iv.length != 16) {
            throw new ConfiguracionIncompletaException("pagos");
        }
        return new IvParameterSpec(iv);
    }

    private byte[] decodificar(String valor) {
        if (valor == null || valor.isBlank()) {
            return new byte[0];
        }
        try {
            return Base64.getDecoder().decode(valor.trim());
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }
}
