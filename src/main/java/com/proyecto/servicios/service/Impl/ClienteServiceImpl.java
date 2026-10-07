package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.OnboardingProperties;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.validation.ClienteValidador;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.proyecto.servicios.service.NacionalidadService;

@Service
@Slf4j
@Transactional(readOnly = true)
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final ClienteMapper clienteMapper;
    private final ClienteValidador validador;
    private final PasswordEncoder passwordEncoder;
    private final OnboardingProperties propiedades;

    private final NacionalidadService nacionalidadService;

    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              CuentaRepository cuentaRepository,
                              ClienteMapper clienteMapper,
                              ClienteValidador validador,
                              PasswordEncoder passwordEncoder,
                              OnboardingProperties propiedades,
                              NacionalidadService nacionalidadService) {
        this.validador = validador;
        this.passwordEncoder = passwordEncoder;
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
        this.clienteMapper = clienteMapper;
        this.propiedades = propiedades;
        this.nacionalidadService = nacionalidadService;
    }

    // ============================ Alta ============================

    /**
     * Registra al cliente y le abre su cuenta en una sola transacción:
     * o se guardan cliente, domicilio, cuenta y login, o no se guarda nada.
     */
    @Override
    @Transactional
    public ClienteResponse registrar(ClienteRequest request) {
        validador.validarAlta(request);     // 400 si algún dato es inválido
        String nacionalidad = nacionalidadService.obtenerNombreValido(request.getNacionalidad());

        String correo = clienteMapper.normalizarCorreo(request.getCorreo());
        verificarQueNoExista(request.getCurp(), request.getRfc(), correo);

        Cliente cliente = clienteMapper.aEntidad(request);
        cliente.setNacionalidad(nacionalidad);   // se guarda como está escrita en el catálogo
        cliente.abrirCuenta(generarNumeroCuenta(), propiedades.getSaldoInicial());
        cliente.crearAcceso(passwordEncoder.encode(request.getContrasena()));

        // Un solo save: la cascada inserta también el domicilio, la cuenta y el login.
        Cliente guardado = clienteRepository.save(cliente);
        log.info("Cliente registrado id={} con cuenta={}", guardado.getId(), guardado.getCuenta().getNumeroCuenta());
        return clienteMapper.aRespuesta(guardado);
    }

    // ======================== Actualización ========================

    @Override
    @Transactional
    public ClienteResponse actualizar(Integer id, ClienteUpdateRequest request) {
        validador.validarActualizacion(id, request);
        String nacionalidad = nacionalidadService.obtenerNombreValido(request.getNacionalidad());

        Cliente cliente = obtener(clienteRepository.buscarPorId(id), "id " + id);
        if (!cliente.isActivo()) {
            throw new ClienteInactivoException("El cliente " + id + " está dado de baja y no admite cambios");
        }

        String correo = clienteMapper.normalizarCorreo(request.getCorreo());
        if (clienteRepository.existeCorreoEnOtroCliente(correo, id)) {
            throw new ClienteYaRegistradoException("el correo " + correo + " pertenece a otro cliente");
        }

        clienteMapper.copiarDatosModificables(request, cliente);
        // saveAndFlush manda el UPDATE en este momento, para que la respuesta
        // ya traiga la fecha de actualización nueva.
        cliente.setNacionalidad(nacionalidad);
        clienteRepository.saveAndFlush(cliente);
        log.info("Cliente actualizado id={}", id);
        return clienteMapper.aRespuesta(cliente);
    }

    // ========================= Baja lógica =========================

    @Override
    @Transactional
    public void darDeBaja(Integer id) {
        validador.validarId(id);

        Cliente cliente = obtener(clienteRepository.buscarPorId(id), "id " + id);
        if (!cliente.isActivo()) {
            throw new ClienteInactivoException("El cliente " + id + " ya estaba dado de baja");
        }
        cliente.darDeBaja();
        log.info("Cliente dado de baja id={} (cuenta {} inactivada)", id, cliente.getCuenta().getNumeroCuenta());
    }

    // ========================== Consultas ==========================

    @Override
    public List<ClienteResponse> consultarTodos() {
        return convertir(clienteRepository.buscarTodos());
    }

    @Override
    public List<ClienteResponse> consultarActivos() {
        return convertir(clienteRepository.buscarActivos());
    }

    @Override
    public List<ClienteResponse> consultarRegistradosEntre(String desde, String hasta) {
        LocalDate[] rango = validador.validarRango(desde, hasta);
        return convertir(clienteRepository.buscarRegistradosEntre(rango[0], rango[1]));
    }

    @Override
    public ClienteResponse consultarPorId(Integer id) {
        validador.validarId(id);
        return clienteMapper.aRespuesta(obtener(clienteRepository.buscarPorId(id), "id " + id));
    }

    @Override
    public ClienteResponse consultarPorCurp(String curp) {
        validador.validarCurp(curp);
        return clienteMapper.aRespuesta(obtener(clienteRepository.buscarPorCurp(curp), "CURP " + curp));
    }

    @Override
    public ClienteResponse consultarPorRfc(String rfc) {
        validador.validarRfc(rfc);
        return clienteMapper.aRespuesta(obtener(clienteRepository.buscarPorRfc(rfc), "RFC " + rfc));
    }

    @Override
    public ClienteResponse consultarPorCorreo(String correo) {
        validador.validarCorreo(correo);
        String normalizado = clienteMapper.normalizarCorreo(correo);
        return clienteMapper.aRespuesta(obtener(clienteRepository.buscarPorCorreo(normalizado), "correo " + normalizado));
    }

    @Override
    public ClienteResponse consultarPorNumeroCuenta(String numeroCuenta) {
        validador.validarNumeroCuenta(numeroCuenta);
        return clienteMapper.aRespuesta(
                obtener(clienteRepository.buscarPorNumeroCuenta(numeroCuenta), "número de cuenta " + numeroCuenta));
    }

    // ===================== Métodos de apoyo =====================

    private void verificarQueNoExista(String curp, String rfc, String correo) {
        if (clienteRepository.existeCurp(curp)) {
            throw new CurpDuplicadaException(curp);
        }
        if (clienteRepository.existeRfc(rfc)) {
            throw new RfcDuplicadoException(rfc);
        }
        if (clienteRepository.existeCorreo(correo)) {
            throw new ClienteYaRegistradoException("ya existe un cliente con el correo " + correo);
        }
    }

    private String generarNumeroCuenta() {
        return String.valueOf(cuentaRepository.siguienteNumeroCuenta());
    }

    private Cliente obtener(Optional<Cliente> resultado, String criterio) {
        return resultado.orElseThrow(() -> new ClienteNoEncontradoException(criterio));
    }

    private List<ClienteResponse> convertir(List<Cliente> clientes) {
        return clientes.stream().map(clienteMapper::aRespuesta).toList();
    }
}
