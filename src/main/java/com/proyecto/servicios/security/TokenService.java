package com.proyecto.servicios.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.proyecto.servicios.exception.NoAutenticadoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Crea y verifica los tokens de sesión (JWT firmado con HMAC-SHA256).
 * El token guarda el id del cliente y su fecha de expiración; nadie puede
 * cambiarlos sin invalidar la firma.
 */
@Component
public class TokenService {

    private static final String EMISOR = "onboarding-clientes";

    private final Algorithm algoritmo;
    private final JWTVerifier verificador;
    private final long minutosVigencia;

    public TokenService(@Value("${seguridad.jwt.secreto}") String secreto,
                        @Value("${seguridad.jwt.minutos-vigencia:30}") long minutosVigencia) {
        if (secreto == null || secreto.length() < 32) {
            throw new IllegalStateException("seguridad.jwt.secreto debe tener al menos 32 caracteres");
        }
        this.algoritmo = Algorithm.HMAC256(secreto);
        this.verificador = JWT.require(algoritmo).withIssuer(EMISOR).build();
        this.minutosVigencia = minutosVigencia;
    }

    public String generar(Integer clienteId) {
        Instant ahora = Instant.now();
        return JWT.create()
                .withIssuer(EMISOR)
                .withSubject(String.valueOf(clienteId))
                .withIssuedAt(ahora)
                .withExpiresAt(ahora.plus(minutosVigencia, ChronoUnit.MINUTES))
                .sign(algoritmo);
    }

    /** Devuelve el id del cliente dueño del token, o lanza 401 si el token no sirve. */
    public Integer obtenerClienteId(String token) {
        try {
            return Integer.valueOf(verificador.verify(token).getSubject());
        } catch (TokenExpiredException e) {
            throw new NoAutenticadoException("El token de sesión expiró. Inicia sesión de nuevo");
        } catch (JWTVerificationException | NumberFormatException e) {
            throw new NoAutenticadoException("El token de sesión no es válido");
        }
    }

    public long getMinutosVigencia() {
        return minutosVigencia;
    }
}