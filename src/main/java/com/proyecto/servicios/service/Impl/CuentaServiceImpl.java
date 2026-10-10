package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.model.onboarding.SaldoResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.validation.ClienteValidador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.proyecto.servicios.security.ControlAcceso;

@Service
@Transactional(readOnly = true)
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteMapper clienteMapper;
    private final ClienteValidador validador;
    private final ControlAcceso controlAcceso;

    public CuentaServiceImpl(CuentaRepository cuentaRepository, ClienteMapper clienteMapper, ClienteValidador validador, ControlAcceso controlAcceso) {
        this.validador = validador;
        this.cuentaRepository = cuentaRepository;
        this.clienteMapper = clienteMapper;
        this.controlAcceso = controlAcceso;
    }

    @Override
    public CuentaResponse consultarPorNumero(String numeroCuenta) {
        validador.validarNumeroCuenta(numeroCuenta);
        Cuenta cuenta = obtenerCuentaPropia(numeroCuenta);
        return clienteMapper.aRespuestaCuenta(cuenta);
    }

    @Override
    public List<CuentaResponse> consultarActivas() {
        Integer clienteAutenticado = controlAcceso.obtenerClienteAutenticado();
        return cuentaRepository.buscarPorEstatusYCliente(EstatusCuenta.ACTIVA, clienteAutenticado).stream()
                .map(clienteMapper::aRespuestaCuenta)
                .toList();
    }

    @Override
    public SaldoResponse consultarSaldo(String numeroCuenta) {
        validador.validarNumeroCuenta(numeroCuenta);
        Cuenta cuenta = obtenerCuentaPropia(numeroCuenta);
        return new SaldoResponse(numeroCuenta, cuenta.getSaldo());
    }

    private Cuenta obtenerCuentaPropia(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.buscarPorNumero(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
        controlAcceso.verificarPropietario(cuenta.getCliente().getId());
        return cuenta;
    }
}
