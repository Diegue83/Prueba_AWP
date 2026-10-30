package com.proyecto.servicios.security;

import com.proyecto.servicios.config.SegundoFactorProperties;
import com.proyecto.servicios.exception.ConfiguracionIncompletaException;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra en reposo los secretos TOTP con AES-256-GCM. Cada valor lleva su propio IV
 * aleatorio al inicio; GCM además detecta si el dato fue alterado en la base.
 */
@Component
public class CifradoSecretos {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int BYTES_IV = 12;
    private static final int BITS_TAG = 128;

    private final SegundoFactorProperties properties;
    private final SecureRandom random = new SecureRandom();

    public CifradoSecretos(SegundoFactorProperties properties) {
        this.properties = properties;
    }

    public String cifrar(byte[] dato) {
        try {
            byte[] iv = new byte[BYTES_IV];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, llave(), new GCMParameterSpec(BITS_TAG, iv));
            byte[] cifrado = cipher.doFinal(dato);
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No fue posible cifrar el secreto", e);
        }
    }

    public byte[] descifrar(String valor) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(valor));
            byte[] iv = new byte[BYTES_IV];
            buffer.get(iv);
            byte[] cifrado = new byte[buffer.remaining()];
            buffer.get(cifrado);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, llave(), new GCMParameterSpec(BITS_TAG, iv));
            return cipher.doFinal(cifrado);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No fue posible descifrar el secreto almacenado", e);
        }
    }

    private SecretKeySpec llave() {
        String clave = properties.getClaveCifrado();
        byte[] bytes;
        try {
            bytes = clave == null ? new byte[0] : Base64.getDecoder().decode(clave.trim());
        } catch (IllegalArgumentException e) {
            bytes = new byte[0];
        }
        if (bytes.length != 32) {
            throw new ConfiguracionIncompletaException("segundo factor");
        }
        return new SecretKeySpec(bytes, "AES");
    }
}
