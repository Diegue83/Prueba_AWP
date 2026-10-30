package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.Impl.GestoPagoTokenServiceImpl;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestoPagoTokenServiceImplTest {

    @Mock
    private GestoPagoAuthClient authClient;
    @Mock
    private GestoPagoTokenRepository tokenRepository;
    @Mock
    private GestoPagoTokenMapper tokenMapper;

    private GestoPagoTokenServiceImpl service;

    @BeforeEach
    void preparar() {
        service = new GestoPagoTokenServiceImpl(authClient, tokenRepository, tokenMapper);
        ReflectionTestUtils.setField(service, "idDistribuidor", 83);
        ReflectionTestUtils.setField(service, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(service, "password", "password-de-prueba");
    }

    @Test
    void obtieneTokenVigenteDePostgresSinAutenticarOtraVez() {
        GestoPagoToken tokenGuardado = new GestoPagoToken();
        tokenGuardado.setToken("token-persistido");
        tokenGuardado.setTokenType("Bearer");
        tokenGuardado.setExpiresIn(3600L);
        tokenGuardado.setFechaActualizacion(LocalDateTime.now().minusMinutes(5));
        tokenGuardado.setActivo(true);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.of(tokenGuardado));

        GestoPagoToken token = service.obtenerTokenVigente();

        assertThat(token).isSameAs(tokenGuardado);
        assertThat(token.getToken()).isEqualTo("token-persistido");
        verify(authClient, never()).authenticate(any(), any(), any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void renuevaYGuardaEnPostgresElTokenVencidoAntesDeDevolverlo() {
        GestoPagoToken tokenVencido = new GestoPagoToken();
        tokenVencido.setToken("token-vencido");
        tokenVencido.setExpiresIn(3600L);
        tokenVencido.setFechaActualizacion(LocalDateTime.now().minusHours(2));
        tokenVencido.setActivo(true);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.of(tokenVencido));

        GestoPagoAuthResponse authResponse = new GestoPagoAuthResponse();
        authResponse.setToken("token-renovado");
        authResponse.setTokenType("Bearer");
        authResponse.setExpiresIn(3600L);
        when(authClient.authenticate(83, "GPS83-TPV-17", "password-de-prueba"))
                .thenReturn(authResponse);
        doAnswer(invocation -> {
            GestoPagoAuthResponse response = invocation.getArgument(0);
            GestoPagoToken target = invocation.getArgument(1);
            target.setToken(response.getToken());
            target.setTokenType(response.getTokenType());
            target.setExpiresIn(response.getExpiresIn());
            return null;
        }).when(tokenMapper).updateEntity(eq(authResponse), eq(tokenVencido));
        when(tokenRepository.save(tokenVencido)).thenReturn(tokenVencido);

        GestoPagoToken token = service.obtenerTokenVigente();

        assertThat(token.getToken()).isEqualTo("token-renovado");
        assertThat(token.getActivo()).isTrue();
        verify(tokenRepository).save(tokenVencido);
    }

    @Test
    void traduceElRechazoDeCredencialesSinExponerLaContrasena() {
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.empty());
        Request request = Request.create(Request.HttpMethod.POST, "https://gestopago/authenticate",
                Map.of(), null, StandardCharsets.UTF_8, null);
        when(authClient.authenticate(83, "GPS83-TPV-17", "password-de-prueba"))
                .thenThrow(new FeignException.Unauthorized("401", request, null, Map.of()));

        assertThatThrownBy(() -> service.obtenerTokenVigente())
                .isInstanceOf(GestoPagoAutenticacionException.class)
                .hasMessageNotContaining("password-de-prueba");
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void rechazaRespuestaDeAutenticacionSinExito() {
        GestoPagoAuthResponse authResponse = new GestoPagoAuthResponse();
        authResponse.setSuccess(false);
        authResponse.setToken("INVALID");
        when(authClient.authenticate(83, "GPS83-TPV-17", "password-de-prueba")).thenReturn(authResponse);

        assertThatThrownBy(() -> service.solicitarNuevoToken())
                .isInstanceOf(GestoPagoAutenticacionException.class);
        verify(tokenRepository, never()).save(any());
    }
}
