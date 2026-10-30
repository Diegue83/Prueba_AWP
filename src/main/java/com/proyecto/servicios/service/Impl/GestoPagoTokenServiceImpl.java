package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.client.GestoPagoErrores;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private static final String OPERACION_AUTENTICACION = "authenticate";

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password}")
    private String password;

    /** La respuesta de authenticate no trae expires_in; se usa la vigencia documentada (24 h) con margen. */
    @Value("${gestopago.auth.vigencia-horas:23}")
    private long horasVigencia;

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoTokenRepository tokenRepository,
                                     GestoPagoTokenMapper tokenMapper) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
    }

    /**
     * PuntoRed entrega tokens válidos por 24 h y no permite pedir uno nuevo en cada petición
     * ni varias veces al día: aquí solo se revisa cada hora y se renueva cuando ya venció.
     */
    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.revision-ms:3600000}", initialDelay = 0)
    public void asegurarTokenVigente() {
        try {
            obtenerTokenVigente();
        } catch (Exception e) {
            log.error("Error al obtener el token GestoPago: {}", e.getMessage());
        }
    }

    @Override
    public GestoPagoToken obtenerTokenVigente() {
        Optional<GestoPagoToken> tokenGuardado = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .filter(this::esVigente);
        if (tokenGuardado.isPresent()) {
            return tokenGuardado.get();
        }

        log.info("No hay token GestoPago vigente en PostgreSQL; se solicitará uno nuevo");
        return autenticarYGuardarToken();
    }

    @Override
    public GestoPagoToken solicitarNuevoToken() {
        log.info("Solicitando un token nuevo a GestoPago para distribuidor={}", idDistribuidor);
        return autenticarYGuardarToken();
    }

    private GestoPagoToken autenticarYGuardarToken() {
        GestoPagoAuthResponse response;
        try {
            response = gestoPagoAuthClient.authenticate(idDistribuidor, codigoDispositivo, password);
        } catch (FeignException e) {
            log.error("Falló la autenticación con GestoPago: HTTP {} ({})", e.status(), e.getClass().getSimpleName());
            throw GestoPagoErrores.traducir(e, OPERACION_AUTENTICACION);
        }
        if (response == null || Boolean.FALSE.equals(response.getSuccess())
                || response.getToken() == null || response.getToken().isBlank()) {
            throw new GestoPagoAutenticacionException(OPERACION_AUTENTICACION);
        }

        GestoPagoToken tokenEntity = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .map(existing -> {
                    tokenMapper.updateEntity(response, existing);
                    existing.setActivo(true);
                    return existing;
                })
                .orElseGet(() -> {
                    GestoPagoToken nuevo = tokenMapper.toEntity(response);
                    nuevo.setIdDistribuidor(idDistribuidor);
                    nuevo.setCodigoDispositivo(codigoDispositivo);
                    nuevo.setActivo(true);
                    return nuevo;
                });

        return tokenRepository.save(tokenEntity);
    }

    private boolean esVigente(GestoPagoToken token) {
        if (!Boolean.TRUE.equals(token.getActivo())
                || token.getToken() == null
                || token.getToken().isBlank()) {
            return false;
        }
        LocalDateTime fechaActualizacion = token.getFechaActualizacion();
        if (fechaActualizacion == null) {
            return false;
        }
        Long expiresIn = token.getExpiresIn();
        long segundos = expiresIn != null && expiresIn > 0 ? expiresIn : horasVigencia * 3600L;
        return fechaActualizacion.plusSeconds(segundos).isAfter(LocalDateTime.now());
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
    }
}
