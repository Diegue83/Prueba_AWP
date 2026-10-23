package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.onboarding.CuentaRequest;
import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.model.onboarding.ConsultaCuentaRequest;
import com.proyecto.servicios.model.onboarding.SaldoResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.validation.ClienteValidador;
import org.springframework.stereotype.Service;

import java.util.List;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.security.ControlAcceso;

@Service
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
    public GenericResponse consultarPorNumero(CuentaRequest request) {
        String numeroCuenta = request.getNumeroCuenta();
        validador.validarNumeroCuenta(numeroCuenta);
        Cuenta cuenta = obtenerCuentaPropia(numeroCuenta);
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Consulta de cuenta realizada correctamente");
        respuesta.setDatos(clienteMapper.aRespuestaCuenta(cuenta));
        return respuesta;
    }

    @Override
    public GenericResponse consultar(ConsultaCuentaRequest request) {
        if (request.getFechaAperturaDesde() != null && request.getFechaAperturaHasta() != null
                && request.getFechaAperturaDesde().isAfter(request.getFechaAperturaHasta())) {
            throw new ValidacionException(
                    "El rango de fechas de apertura no es válido",
                    List.of("fechaAperturaDesde: debe ser igual o anterior a fechaAperturaHasta"));
        }
        Integer clienteId = controlAcceso.obtenerClienteAutenticado();
        List<CuentaResponse> cuentas = cuentaRepository.buscarPorCriterios(
                        clienteId,
                        request.getNumeroCuenta(),
                        request.getEstatus(),
                        request.getFechaAperturaDesde(),
                        request.getFechaAperturaHasta())
                .stream()
                .map(clienteMapper::aRespuestaCuenta)
                .toList();
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Consulta de cuentas realizada correctamente");
        respuesta.setDatos(cuentas);
        return respuesta;
    }

    @Override
    public GenericResponse consultarActivas() {
        Integer clienteAutenticado = controlAcceso.obtenerClienteAutenticado();
        List<CuentaResponse> cuentas = cuentaRepository.buscarPorEstatusYCliente(EstatusCuenta.ACTIVA, clienteAutenticado).stream()
                .map(clienteMapper::aRespuestaCuenta)
                .toList();
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Consulta de cuentas activas realizada correctamente");
        respuesta.setDatos(cuentas);
        return respuesta;
    }

    @Override
    public GenericResponse consultarSaldo(CuentaRequest request) {
        String numeroCuenta = request.getNumeroCuenta();
        validador.validarNumeroCuenta(numeroCuenta);
        Cuenta cuenta = obtenerCuentaPropia(numeroCuenta);
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Consulta de saldo realizada correctamente");
        respuesta.setDatos(new SaldoResponse(numeroCuenta, cuenta.getSaldo()));
        return respuesta;
    }

    private Cuenta obtenerCuentaPropia(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.buscarPorNumero(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
        controlAcceso.verificarPropietario(cuenta.getCliente().getId());
        return cuenta;
    }
}
