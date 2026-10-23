package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.OnboardingProperties;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ConsultaClienteRequest;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.validation.ClienteValidador;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.proyecto.servicios.service.NacionalidadService;
import com.proyecto.servicios.security.ControlAcceso;

@Service
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final ClienteMapper clienteMapper;
    private final ClienteValidador validador;
    private final PasswordEncoder passwordEncoder;
    private final OnboardingProperties propiedades;
    private final ControlAcceso controlAcceso;

    private final NacionalidadService nacionalidadService;

    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              CuentaRepository cuentaRepository,
                              ClienteMapper clienteMapper,
                              ClienteValidador validador,
                              PasswordEncoder passwordEncoder,
                              OnboardingProperties propiedades,
                              NacionalidadService nacionalidadService,
                              ControlAcceso controlAcceso
    ) {
        this.validador = validador;
        this.passwordEncoder = passwordEncoder;
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
        this.clienteMapper = clienteMapper;
        this.propiedades = propiedades;
        this.nacionalidadService = nacionalidadService;
        this.controlAcceso = controlAcceso;
    }

    // ============================ Alta ============================

    @Override
    public GenericResponse registrar(ClienteRequest request) {
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
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Cliente registrado correctamente");
        respuesta.setDatos(clienteMapper.aRespuesta(guardado));
        return respuesta;
    }

    // ======================== Actualización ========================

    @Override
    public GenericResponse actualizar(ClienteRequest request) {
        validador.validarActualizacion(request);
        Integer id = request.getId();
        controlAcceso.verificarPropietario(id);
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
        cliente = clienteRepository.saveAndFlush(cliente);
        log.info("Cliente actualizado id={}", id);
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Cliente actualizado correctamente");
        respuesta.setDatos(clienteMapper.aRespuesta(cliente));
        return respuesta;
    }

    // ========================= Baja lógica =========================

    @Override
    public GenericResponse darDeBaja(ClienteRequest request) {
        validador.validarBaja(request);
        Integer id = request.getId();
        controlAcceso.verificarPropietario(id);

        Cliente cliente = obtener(clienteRepository.buscarPorId(id), "id " + id);
        if (!cliente.isActivo()) {
            throw new ClienteInactivoException("El cliente " + id + " ya estaba dado de baja");
        }
        cliente.darDeBaja();
        clienteRepository.save(cliente);
        log.info("Cliente dado de baja id={} (cuenta {} inactivada)", id, cliente.getCuenta().getNumeroCuenta());
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Cliente dado de baja correctamente");
        respuesta.setDatos(null);
        return respuesta;
    }

    // ========================== Consultas ==========================

    @Override
    public GenericResponse consultarTodos() {
        List<ClienteResponse> clientes = clienteAutenticado()
                .map(clienteMapper::aRespuesta)
                .map(List::of)
                .orElseGet(List::of);
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Consulta de clientes realizada correctamente");
        respuesta.setDatos(clientes);
        return respuesta;
    }

    @Override
    public GenericResponse consultar(ClienteRequest criterios) {
        validador.validarConsulta(criterios);
        if (criterios.getFechaRegistroDesde() != null && criterios.getFechaRegistroHasta() != null
                && criterios.getFechaRegistroDesde().isAfter(criterios.getFechaRegistroHasta())) {
            throw new ValidacionException(
                    "El rango de fechas de registro no es válido",
                    List.of("fechaRegistroDesde: debe ser igual o anterior a fechaRegistroHasta"));
        }

        Integer clienteId = controlAcceso.obtenerClienteAutenticado();
        String correo = clienteMapper.normalizarCorreo(criterios.getCorreo());
        List<ClienteResponse> clientes = clienteRepository.buscarPorCriterios(
                        clienteId,
                        criterios.getId(),
                        normalizarIdentificador(criterios.getCurp()),
                        normalizarIdentificador(criterios.getRfc()),
                        correo,
                        criterios.getNumeroCuenta(),
                        criterios.getActivo(),
                        criterios.getFechaRegistroDesde(),
                        criterios.getFechaRegistroHasta())
                .stream()
                .map(clienteMapper::aRespuesta)
                .toList();
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Consulta de clientes realizada correctamente");
        respuesta.setDatos(clientes);
        return respuesta;
    }

    @Override
    public GenericResponse consultar(ConsultaClienteRequest criterios) {
        ClienteRequest requestExistente = new ClienteRequest();
        requestExistente.setId(criterios.getId());
        requestExistente.setCurp(criterios.getCurp());
        requestExistente.setRfc(criterios.getRfc());
        requestExistente.setCorreo(criterios.getCorreo());
        requestExistente.setNumeroCuenta(criterios.getNumeroCuenta());
        requestExistente.setActivo(criterios.getActivo());
        requestExistente.setFechaRegistroDesde(criterios.getFechaRegistroDesde());
        requestExistente.setFechaRegistroHasta(criterios.getFechaRegistroHasta());
        return consultar(requestExistente);
    }

    @Override
    public GenericResponse consultarActivos() {
        List<ClienteResponse> clientes = clienteAutenticado()
                .filter(Cliente::isActivo)
                .map(clienteMapper::aRespuesta)
                .map(List::of)
                .orElseGet(List::of);
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Consulta de clientes activos realizada correctamente");
        respuesta.setDatos(clientes);
        return respuesta;
    }

    @Override
    public GenericResponse consultarRegistradosEntre(String desde, String hasta) {
        LocalDate[] rango = validador.validarRango(desde, hasta);
        List<ClienteResponse> clientes = clienteAutenticado()
                .filter(cliente -> !cliente.getFechaRegistro().isBefore(rango[0])
                        && !cliente.getFechaRegistro().isAfter(rango[1]))
                .map(clienteMapper::aRespuesta)
                .map(List::of)
                .orElseGet(List::of);
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Consulta de clientes por fecha realizada correctamente");
        respuesta.setDatos(clientes);
        return respuesta;
    }

    @Override
    public GenericResponse consultarPorId(Integer id) {
        validador.validarId(id);
        controlAcceso.verificarPropietario(id);
        ClienteResponse cliente = clienteMapper.aRespuesta(obtener(clienteRepository.buscarPorId(id), "id " + id));
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Cliente encontrado");
        respuesta.setDatos(cliente);
        return respuesta;
    }

    @Override
    public GenericResponse consultarPorCurp(String curp) {
        validador.validarCurp(curp);
        ClienteResponse cliente = responderSiEsPropietario(obtener(clienteRepository.buscarPorCurp(curp), "CURP " + curp));
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Cliente encontrado");
        respuesta.setDatos(cliente);
        return respuesta;
    }

    @Override
    public GenericResponse consultarPorRfc(String rfc) {
        validador.validarRfc(rfc);
        ClienteResponse cliente = responderSiEsPropietario(obtener(clienteRepository.buscarPorRfc(rfc), "RFC " + rfc));
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Cliente encontrado");
        respuesta.setDatos(cliente);
        return respuesta;
    }

    @Override
    public GenericResponse consultarPorCorreo(String correo) {
        validador.validarCorreo(correo);
        String normalizado = clienteMapper.normalizarCorreo(correo);
        ClienteResponse cliente = responderSiEsPropietario(obtener(clienteRepository.buscarPorCorreo(normalizado), "correo " + normalizado));
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Cliente encontrado");
        respuesta.setDatos(cliente);
        return respuesta;
    }

    @Override
    public GenericResponse consultarPorNumeroCuenta(String numeroCuenta) {
        validador.validarNumeroCuenta(numeroCuenta);
        ClienteResponse cliente = responderSiEsPropietario(
                obtener(clienteRepository.buscarPorNumeroCuenta(numeroCuenta), "número de cuenta " + numeroCuenta));
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Cliente encontrado");
        respuesta.setDatos(cliente);
        return respuesta;
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

    private Optional<Cliente> clienteAutenticado() {
        return clienteRepository.buscarPorId(controlAcceso.obtenerClienteAutenticado());
    }

    private ClienteResponse responderSiEsPropietario(Cliente cliente) {
        controlAcceso.verificarPropietario(cliente.getId());
        return clienteMapper.aRespuesta(cliente);
    }

    private String normalizarIdentificador(String identificador) {
        return identificador == null ? null : identificador.trim().toUpperCase(Locale.ROOT);
    }
}
