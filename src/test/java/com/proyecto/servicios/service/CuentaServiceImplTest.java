package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.CuentaRequest;
import com.proyecto.servicios.model.onboarding.ConsultaCuentaRequest;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.security.ControlAcceso;
import com.proyecto.servicios.service.Impl.CuentaServiceImpl;
import com.proyecto.servicios.validation.ClienteValidador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

    private static final String NUMERO_CUENTA = "1000000001";

    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private ControlAcceso controlAcceso;

    private CuentaServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new CuentaServiceImpl(cuentaRepository, new ClienteMapper(),
                new ClienteValidador(), controlAcceso);
    }

    @Test
    void rechazaLaConsultaDeLaCuentaDeOtroCliente() {
        Cuenta cuenta = mock(Cuenta.class);
        Cliente cliente = mock(Cliente.class);
        when(cuenta.getCliente()).thenReturn(cliente);
        when(cliente.getId()).thenReturn(2);
        when(cuentaRepository.buscarPorNumero(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));
        doThrow(new AccesoDenegadoException()).when(controlAcceso).verificarPropietario(2);

        CuentaRequest request = new CuentaRequest();
        request.setNumeroCuenta(NUMERO_CUENTA);
        assertThatThrownBy(() -> servicio.consultarPorNumero(request))
                .isInstanceOf(AccesoDenegadoException.class);

        verify(controlAcceso).verificarPropietario(2);
    }

    @Test
    void lasCuentasActivasSeLimitanAlTitularAutenticado() {
        when(controlAcceso.obtenerClienteAutenticado()).thenReturn(1);
        when(cuentaRepository.buscarPorEstatusYCliente(EstatusCuenta.ACTIVA, 1))
                .thenReturn(List.of());

        assertThat((List<?>) servicio.consultarActivas().getDatos()).isEmpty();
    }

    @Test
    void consultaCombinaFiltrosYLimitaResultadosAlTitularAutenticado() {
        LocalDate desde = LocalDate.parse("2025-01-01");
        LocalDate hasta = LocalDate.parse("2025-12-31");
        when(controlAcceso.obtenerClienteAutenticado()).thenReturn(1);
        when(cuentaRepository.buscarPorCriterios(1, NUMERO_CUENTA, EstatusCuenta.ACTIVA, desde, hasta))
                .thenReturn(List.of());

        ConsultaCuentaRequest request = new ConsultaCuentaRequest();
        request.setNumeroCuenta(NUMERO_CUENTA);
        request.setEstatus(EstatusCuenta.ACTIVA);
        request.setFechaAperturaDesde(desde);
        request.setFechaAperturaHasta(hasta);

        assertThat((List<?>) servicio.consultar(request).getDatos()).isEmpty();
        verify(cuentaRepository).buscarPorCriterios(1, NUMERO_CUENTA, EstatusCuenta.ACTIVA, desde, hasta);
    }

    @Test
    void consultaRechazaRangoDeAperturaInvertido() {
        ConsultaCuentaRequest request = new ConsultaCuentaRequest();
        request.setFechaAperturaDesde(LocalDate.parse("2025-12-31"));
        request.setFechaAperturaHasta(LocalDate.parse("2025-01-01"));

        assertThatThrownBy(() -> servicio.consultar(request))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("rango de fechas");
    }
}
