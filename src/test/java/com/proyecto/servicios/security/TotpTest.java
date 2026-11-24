package com.proyecto.servicios.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TotpTest {

    /** Secreto de los vectores de prueba del RFC 6238 (apéndice B) para HMAC-SHA1. */
    private static final byte[] SECRETO_RFC = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    @Test
    void generaLosCodigosDelRfc6238() {
        Totp totp = new Totp();

        assertThat(totp.generar(SECRETO_RFC, 59L / 30)).isEqualTo("287082");
        assertThat(totp.generar(SECRETO_RFC, 1111111109L / 30)).isEqualTo("081804");
        assertThat(totp.generar(SECRETO_RFC, 1234567890L / 30)).isEqualTo("005924");
        assertThat(totp.generar(SECRETO_RFC, 2000000000L / 30)).isEqualTo("279037");
    }

    @Test
    void aceptaElCodigoActualYElDelPasoVecinoPorDesfaseDeReloj() {
        Totp totp = en(1111111109L);
        long paso = 1111111109L / 30;

        assertThat(totp.verificar(SECRETO_RFC, "081804", 1)).hasValue(paso);
        assertThat(totp.verificar(SECRETO_RFC, totp.generar(SECRETO_RFC, paso - 1), 1)).hasValue(paso - 1);
        assertThat(totp.verificar(SECRETO_RFC, totp.generar(SECRETO_RFC, paso - 2), 1)).isEmpty();
    }

    @Test
    void rechazaCodigosConFormatoInvalido() {
        Totp totp = en(59L);

        assertThat(totp.verificar(SECRETO_RFC, null, 1)).isEmpty();
        assertThat(totp.verificar(SECRETO_RFC, "28708", 1)).isEmpty();
        assertThat(totp.verificar(SECRETO_RFC, "28708a", 1)).isEmpty();
    }

    @Test
    void generaSecretosAleatoriosDe160Bits() {
        Totp totp = new Totp();

        byte[] secreto = totp.generarSecreto();

        assertThat(secreto).hasSize(20).isNotEqualTo(totp.generarSecreto());
        assertThat(totp.aBase32(secreto)).matches("[A-Z2-7]{32}");
    }

    private Totp en(long epochSegundos) {
        return new Totp(Clock.fixed(Instant.ofEpochSecond(epochSegundos), ZoneOffset.UTC));
    }
}
