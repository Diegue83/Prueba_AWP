package com.proyecto.servicios.security;

import com.proyecto.servicios.config.SegundoFactorProperties;
import com.proyecto.servicios.exception.ConfiguracionIncompletaException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CifradoSecretosTest {

    private static final String CLAVE = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void cifraYDescifraConIvDistintoEnCadaValor() {
        CifradoSecretos cifrado = new CifradoSecretos(propiedades(CLAVE));
        byte[] secreto = "secreto-totp".getBytes();

        String primero = cifrado.cifrar(secreto);
        String segundo = cifrado.cifrar(secreto);

        assertThat(primero).isNotEqualTo(segundo).doesNotContain("secreto");
        assertThat(cifrado.descifrar(primero)).isEqualTo(secreto);
        assertThat(cifrado.descifrar(segundo)).isEqualTo(secreto);
    }

    @Test
    void detectaUnValorAlteradoEnLaBase() {
        CifradoSecretos cifrado = new CifradoSecretos(propiedades(CLAVE));
        byte[] bytes = Base64.getDecoder().decode(cifrado.cifrar("secreto".getBytes()));
        bytes[bytes.length - 1] ^= 1;

        assertThatThrownBy(() -> cifrado.descifrar(Base64.getEncoder().encodeToString(bytes)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sinClaveValidaRespondeServicioNoConfigurado() {
        assertThatThrownBy(() -> new CifradoSecretos(propiedades("")).cifrar(new byte[1]))
                .isInstanceOf(ConfiguracionIncompletaException.class);
        assertThatThrownBy(() -> new CifradoSecretos(propiedades("corta")).cifrar(new byte[1]))
                .isInstanceOf(ConfiguracionIncompletaException.class);
    }

    private SegundoFactorProperties propiedades(String clave) {
        SegundoFactorProperties properties = new SegundoFactorProperties();
        properties.setClaveCifrado(clave);
        return properties;
    }
}
