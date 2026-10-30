package com.proyecto.servicios.service;

import com.proyecto.servicios.config.SegundoFactorProperties;
import com.proyecto.servicios.entity.onboarding.SegundoFactor;
import com.proyecto.servicios.exception.SegundoFactorBloqueadoException;
import com.proyecto.servicios.exception.SegundoFactorInvalidoException;
import com.proyecto.servicios.exception.SegundoFactorNoConfiguradoException;
import com.proyecto.servicios.exception.SegundoFactorYaActivoException;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.SegundoFactorRepository;
import com.proyecto.servicios.security.CifradoSecretos;
import com.proyecto.servicios.security.Totp;
import com.proyecto.servicios.service.Impl.SegundoFactorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Optional;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SegundoFactorServiceImplTest {

    private static final int CLIENTE = 7;

    @Mock
    private SegundoFactorRepository repository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private Totp totp;

    private CifradoSecretos cifrado;
    private SegundoFactorServiceImpl service;

    @BeforeEach
    void preparar() {
        SegundoFactorProperties properties = new SegundoFactorProperties();
        properties.setClaveCifrado(Base64.getEncoder().encodeToString(new byte[32]));
        properties.setMaxIntentos(3);
        cifrado = new CifradoSecretos(properties);
        service = new SegundoFactorServiceImpl(repository, clienteRepository, cifrado, totp, properties);
    }

    @Test
    void verificaElCodigoYRegistraElPasoUsado() {
        SegundoFactor factor = activo(100L);
        when(repository.bloquearPorCliente(CLIENTE)).thenReturn(Optional.of(factor));
        when(totp.verificar(any(), anyString(), anyInt())).thenReturn(OptionalLong.of(101L));

        service.verificar(CLIENTE, "123456");

        assertThat(factor.getUltimoPaso()).isEqualTo(101L);
    }

    @Test
    void noAceptaDosVecesElMismoCodigo() {
        SegundoFactor factor = activo(101L);
        when(repository.bloquearPorCliente(CLIENTE)).thenReturn(Optional.of(factor));
        when(totp.verificar(any(), anyString(), anyInt())).thenReturn(OptionalLong.of(101L));

        assertThatThrownBy(() -> service.verificar(CLIENTE, "123456"))
                .isInstanceOf(SegundoFactorInvalidoException.class);
        assertThat(factor.getIntentosFallidos()).isEqualTo((short) 1);
    }

    @Test
    void bloqueaTrasVariosCodigosIncorrectosYYaNoVerificaMas() {
        SegundoFactor factor = activo(0L);
        when(repository.bloquearPorCliente(CLIENTE)).thenReturn(Optional.of(factor));
        when(totp.verificar(any(), anyString(), anyInt())).thenReturn(OptionalLong.empty());

        assertThatThrownBy(() -> service.verificar(CLIENTE, "000000")).isInstanceOf(SegundoFactorInvalidoException.class);
        assertThatThrownBy(() -> service.verificar(CLIENTE, "000000")).isInstanceOf(SegundoFactorInvalidoException.class);
        assertThatThrownBy(() -> service.verificar(CLIENTE, "000000")).isInstanceOf(SegundoFactorBloqueadoException.class);
        assertThatThrownBy(() -> service.verificar(CLIENTE, "999999")).isInstanceOf(SegundoFactorBloqueadoException.class);
    }

    @Test
    void sinSegundoFactorActivoNoPermiteOperar() {
        SegundoFactor pendiente = SegundoFactor.registrar(CLIENTE, cifrado.cifrar(new byte[20]));
        when(repository.bloquearPorCliente(CLIENTE)).thenReturn(Optional.of(pendiente));

        assertThatThrownBy(() -> service.verificar(CLIENTE, "123456"))
                .isInstanceOf(SegundoFactorNoConfiguradoException.class);
        verify(totp, never()).verificar(any(), anyString(), anyInt());
    }

    @Test
    void activaConElPrimerCodigoCorrectoYNoPermiteRegistrarOtroSecreto() {
        SegundoFactor pendiente = SegundoFactor.registrar(CLIENTE, cifrado.cifrar(new byte[20]));
        when(repository.bloquearPorCliente(CLIENTE)).thenReturn(Optional.of(pendiente));
        when(totp.verificar(any(), anyString(), anyInt())).thenReturn(OptionalLong.of(50L));

        service.activar(CLIENTE, "123456");

        assertThat(pendiente.isActivo()).isTrue();
        assertThatThrownBy(() -> service.activar(CLIENTE, "123456")).isInstanceOf(SegundoFactorYaActivoException.class);
    }

    private SegundoFactor activo(long ultimoPaso) {
        SegundoFactor factor = SegundoFactor.registrar(CLIENTE, cifrado.cifrar(new byte[20]));
        factor.activar(ultimoPaso);
        return factor;
    }
}
