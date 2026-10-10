package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.ContrasenaIncorrectaException;
import com.proyecto.servicios.exception.CorreoNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.service.Impl.LoginServiceImpl;
import com.proyecto.servicios.security.TokenService;
import com.proyecto.servicios.validation.ClienteValidador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static com.proyecto.servicios.service.DatosDePrueba.CONTRASENA;
import static com.proyecto.servicios.service.DatosDePrueba.CORREO;
import static com.proyecto.servicios.service.DatosDePrueba.NUMERO_CUENTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private TokenService tokenService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private LoginServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new LoginServiceImpl(clienteRepository, new ClienteMapper(), new ClienteValidador(),
                passwordEncoder, tokenService);
    }

    @Test
    void inicioDeSesionCorrecto() {
        when(clienteRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(clienteRegistrado()));

        LoginResponse respuesta = servicio.iniciarSesion(solicitud("Zayda@Correo.com", CONTRASENA));

        assertThat(respuesta.getCorreo()).isEqualTo(CORREO);
        assertThat(respuesta.getNombreCompleto()).isEqualTo("Zayda Vargas Vargas");
        assertThat(respuesta.getNumeroCuenta()).isEqualTo(NUMERO_CUENTA);
        assertThat(respuesta.getUltimoAcceso()).isEqualTo(LocalDate.now());
    }

    @Test
    void correoNoEncontrado() {
        when(clienteRepository.buscarPorCorreo("nadie@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarSesion(solicitud("nadie@correo.com", CONTRASENA)))
                .isInstanceOf(CorreoNoEncontradoException.class);
    }

    @Test
    void contrasenaErronea() {
        when(clienteRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(clienteRegistrado()));

        assertThatThrownBy(() -> servicio.iniciarSesion(solicitud(CORREO, "Otra12345")))
                .isInstanceOf(ContrasenaIncorrectaException.class);
    }

    @Test
    void clienteDadoDeBajaNoPuedeIniciarSesion() {
        Cliente inactivo = clienteRegistrado();
        inactivo.darDeBaja();
        when(clienteRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> servicio.iniciarSesion(solicitud(CORREO, CONTRASENA)))
                .isInstanceOf(ClienteInactivoException.class);
    }

    @Test
    void datosMalFormadosNoLleganALaBaseDeDatos() {
        assertThatThrownBy(() -> servicio.iniciarSesion(solicitud("sin-arroba", CONTRASENA)))
                .isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> servicio.iniciarSesion(solicitud(CORREO, "")))
                .isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> servicio.iniciarSesion(solicitud(null, null)))
                .isInstanceOf(ValidacionException.class);
        verify(clienteRepository, never()).buscarPorCorreo(any());
    }

    private Cliente clienteRegistrado() {
        return DatosDePrueba.clienteExistente(passwordEncoder.encode(CONTRASENA));
    }

    private LoginRequest solicitud(String correo, String contrasena) {
        LoginRequest solicitud = new LoginRequest();
        solicitud.setCorreo(correo);
        solicitud.setContrasena(contrasena);
        return solicitud;
    }
}
