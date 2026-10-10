package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Login;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.ContrasenaIncorrectaException;
import com.proyecto.servicios.exception.CorreoNoEncontradoException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.service.LoginService;
import com.proyecto.servicios.validation.ClienteValidador;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.proyecto.servicios.security.TokenService;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final ClienteValidador validador;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;


    public LoginServiceImpl(ClienteRepository clienteRepository,
                            ClienteMapper clienteMapper,
                            ClienteValidador validador,
                            PasswordEncoder passwordEncoder,
                            TokenService tokenService
    ) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
        this.validador = validador;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * Pasos del inicio de sesión, cada uno con su propio error:
     *   1. Datos mal formados           -> 400 ERROR_VALIDACION
     *   2. El correo no está registrado -> 404 CORREO_NO_ENCONTRADO
     *   3. El cliente está dado de baja -> 409 CLIENTE_INACTIVO
     *   4. La contraseña no coincide    -> 401 CONTRASENA_INCORRECTA
     */
    @Override
    @Transactional
    public LoginResponse iniciarSesion(LoginRequest request) {
        validador.validarLogin(request);

        String correo = clienteMapper.normalizarCorreo(request.getCorreo());
        Cliente cliente = clienteRepository.buscarPorCorreo(correo)
                .orElseThrow(() -> new CorreoNoEncontradoException(correo));

        if (!cliente.isActivo()) {
            throw new ClienteInactivoException("El cliente está dado de baja y no puede iniciar sesión");
        }

        Login acceso = cliente.getAcceso();
        // matches() cifra la contraseña recibida con la misma sal y compara los hashes:
        // la contraseña guardada nunca se descifra.
        if (!passwordEncoder.matches(request.getContrasena(), acceso.getContrasena())) {
            log.warn("Intento de acceso con contraseña errónea para el cliente id={}", cliente.getId());
            throw new ContrasenaIncorrectaException();
        }

        acceso.registrarAcceso();
        log.info("Inicio de sesión correcto del cliente id={}", cliente.getId());
        return construirRespuesta(cliente);
    }

    private LoginResponse construirRespuesta(Cliente cliente) {
        LoginResponse respuesta = new LoginResponse();
        respuesta.setMensaje("Inicio de sesión correcto");
        respuesta.setClienteId(cliente.getId());
        respuesta.setNombreCompleto(Stream.of(cliente.getNombre(), cliente.getSegundoNombre(),
                        cliente.getApellidoPaterno(), cliente.getApellidoMaterno())
                .filter(parte -> parte != null && !parte.isBlank())
                .collect(Collectors.joining(" ")));
        respuesta.setCorreo(cliente.getCorreo());
        respuesta.setNumeroCuenta(cliente.getCuenta().getNumeroCuenta());
        respuesta.setUltimoAcceso(cliente.getAcceso().getUltimoAcceso());
        respuesta.setToken(tokenService.generar(cliente.getId()));
        respuesta.setTipoToken("Bearer");
        respuesta.setExpiraEnMinutos(tokenService.getMinutosVigencia());
        return respuesta;
    }
}
