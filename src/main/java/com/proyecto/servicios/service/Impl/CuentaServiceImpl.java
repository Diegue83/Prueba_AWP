package com.proyecto.servicios.service.Impl;

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

@Service
@Transactional(readOnly = true)
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteMapper clienteMapper;
    private final ClienteValidador validador;

    public CuentaServiceImpl(CuentaRepository cuentaRepository, ClienteMapper clienteMapper, ClienteValidador validador) {
        this.validador = validador;
        this.cuentaRepository = cuentaRepository;
        this.clienteMapper = clienteMapper;
    }

    @Override
    public CuentaResponse consultarPorNumero(String numeroCuenta) {
        validador.validarNumeroCuenta(numeroCuenta);
        return cuentaRepository.buscarPorNumero(numeroCuenta)
                .map(clienteMapper::aRespuestaCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
    }

    @Override
    public List<CuentaResponse> consultarActivas() {
        return cuentaRepository.buscarPorEstatus(EstatusCuenta.ACTIVA).stream()
                .map(clienteMapper::aRespuestaCuenta)
                .toList();
    }

    @Override
    public SaldoResponse consultarSaldo(String numeroCuenta) {
        validador.validarNumeroCuenta(numeroCuenta);
        return cuentaRepository.consultarSaldo(numeroCuenta)
                .map(saldo -> new SaldoResponse(numeroCuenta, saldo))
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
    }
}
