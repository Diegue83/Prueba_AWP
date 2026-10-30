package com.proyecto.servicios.security;

import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.OptionalLong;

/**
 * Códigos de un solo uso basados en tiempo (RFC 6238): HMAC-SHA1, 6 dígitos, pasos de 30 s.
 * Es el formato que leen Google Authenticator, Microsoft Authenticator, Authy, etc.
 */
@Component
public class Totp {

    private static final int BYTES_SECRETO = 20;
    private static final int SEGUNDOS_POR_PASO = 30;
    private static final int DIGITOS = 6;
    private static final int MODULO = 1_000_000;

    private final SecureRandom random = new SecureRandom();
    private final Clock reloj;

    public Totp() {
        this(Clock.systemUTC());
    }

    Totp(Clock reloj) {
        this.reloj = reloj;
    }

    public byte[] generarSecreto() {
        byte[] secreto = new byte[BYTES_SECRETO];
        random.nextBytes(secreto);
        return secreto;
    }

    public String aBase32(byte[] secreto) {
        return new Base32().encodeToString(secreto).replace("=", "");
    }

    public long pasoActual() {
        return reloj.instant().getEpochSecond() / SEGUNDOS_POR_PASO;
    }

    /**
     * Busca el código en el paso actual y en los vecinos que permite la ventana (desfase de reloj
     * del teléfono). Devuelve el paso que coincidió para que quien llama impida reutilizarlo.
     */
    public OptionalLong verificar(byte[] secreto, String codigo, int ventana) {
        if (codigo == null || !codigo.matches("\\d{" + DIGITOS + "}")) {
            return OptionalLong.empty();
        }
        byte[] esperado = codigo.getBytes();
        long actual = pasoActual();
        for (long paso = actual - ventana; paso <= actual + ventana; paso++) {
            if (MessageDigest.isEqual(esperado, generar(secreto, paso).getBytes())) {
                return OptionalLong.of(paso);
            }
        }
        return OptionalLong.empty();
    }

    String generar(byte[] secreto, long paso) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secreto, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(paso).array());
            int desplazamiento = hash[hash.length - 1] & 0x0f;
            int binario = ((hash[desplazamiento] & 0x7f) << 24)
                    | ((hash[desplazamiento + 1] & 0xff) << 16)
                    | ((hash[desplazamiento + 2] & 0xff) << 8)
                    | (hash[desplazamiento + 3] & 0xff);
            return String.format("%0" + DIGITOS + "d", binario % MODULO);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 no está disponible en la JVM", e);
        }
    }
}
