package com.proyecto.servicios.service;

import com.proyecto.servicios.config.OnboardingProperties;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.enums.Sexo;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import com.proyecto.servicios.validation.ClienteValidador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static com.proyecto.servicios.service.DatosDePrueba.CONTRASENA;
import static com.proyecto.servicios.service.DatosDePrueba.CORREO;
import static com.proyecto.servicios.service.DatosDePrueba.CURP;
import static com.proyecto.servicios.service.DatosDePrueba.NUMERO_CUENTA;
import static com.proyecto.servicios.service.DatosDePrueba.RFC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.proyecto.servicios.service.NacionalidadService;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private NacionalidadService nacionalidadService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private OnboardingProperties propiedades;
    private ClienteServiceImpl servicio;

    @BeforeEach
    void preparar() {
        propiedades = new OnboardingProperties();
        propiedades.setSaldoInicial(new BigDecimal("500"));
        lenient().when(nacionalidadService.obtenerNombreValido(anyString())).thenReturn("Mexicana");
        servicio = new ClienteServiceImpl(clienteRepository, cuentaRepository, new ClienteMapper(),
                new ClienteValidador(), passwordEncoder, propiedades, nacionalidadService);
    }

    // ------------------------------ Alta ------------------------------

    @Test
    void registrarCreaClienteConDomicilioCuentaActivaYLogin() {
        when(cuentaRepository.siguienteNumeroCuenta()).thenReturn(1000000001L);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ClienteResponse respuesta = servicio.registrar(DatosDePrueba.solicitudDeAlta());

        assertThat(respuesta.isActivo()).isTrue();
        assertThat(respuesta.getCurp()).isEqualTo(CURP);
        assertThat(respuesta.getCorreo()).isEqualTo(CORREO);                 // normalizado a minúsculas
        assertThat(respuesta.getSexo()).isEqualTo(Sexo.MUJER);
        assertThat(respuesta.getEstadoCivil()).isEqualTo(EstadoCivil.SOLTERO);   // llegó como "S"
        assertThat(respuesta.getIngresoMensual().toPlainString()).isEqualTo("15000.50");   // 2 decimales
        assertThat(respuesta.getDomicilio().getCodigoPostal()).isEqualTo("37800");
        assertThat(respuesta.getCuenta().getNumeroCuenta()).isEqualTo(NUMERO_CUENTA);
        assertThat(respuesta.getCuenta().getSaldo().toPlainString()).isEqualTo("500.00");   // 2 decimales
        assertThat(respuesta.getCuenta().getEstatus()).isEqualTo(EstatusCuenta.ACTIVA);

        // La contraseña se guarda cifrada, nunca en claro.
        ArgumentCaptor<Cliente> guardado = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(guardado.capture());
        String hash = guardado.getValue().getAcceso().getContrasena();
        assertThat(hash).isNotEqualTo(CONTRASENA).hasSize(60);
        assertThat(passwordEncoder.matches(CONTRASENA, hash)).isTrue();
    }

    @Test
    void registrarConDatosInvalidosRespondeTodosLosErroresJuntos() {
        ClienteRequest solicitud = DatosDePrueba.solicitudDeAlta();
        solicitud.setNombre("Zayda3");
        solicitud.setTelefonoMovil("123");
        solicitud.setIngresoMensual(new BigDecimal("10.999"));
        solicitud.getDomicilio().setCodigoPostal("3780");
        solicitud.setContrasena("corta");

        ValidacionException error = catchThrowableOfType(
                () -> servicio.registrar(solicitud), ValidacionException.class);

        assertThat(error).isNotNull();
        assertThat(error.getDetalles())
                .anyMatch(detalle -> detalle.startsWith("nombre:"))
                .anyMatch(detalle -> detalle.startsWith("telefonoMovil:"))
                .anyMatch(detalle -> detalle.startsWith("ingresoMensual:"))
                .anyMatch(detalle -> detalle.startsWith("domicilio.codigoPostal:"))
                .anyMatch(detalle -> detalle.startsWith("contrasena:"));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void registrarRechazaCurpQueNoCoincideConLaFechaDeNacimiento() {
        ClienteRequest solicitud = DatosDePrueba.solicitudDeAlta();
        solicitud.setFechaNacimiento("1995-03-13");

        assertThatThrownBy(() -> servicio.registrar(solicitud)).isInstanceOf(ValidacionException.class);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void registrarRechazaCurpDuplicada() {
        when(clienteRepository.existeCurp(CURP)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(DatosDePrueba.solicitudDeAlta()))
                .isInstanceOf(CurpDuplicadaException.class);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void registrarRechazaRfcDuplicado() {
        when(clienteRepository.existeRfc(RFC)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(DatosDePrueba.solicitudDeAlta()))
                .isInstanceOf(RfcDuplicadoException.class);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void registrarRechazaCorreoDuplicado() {
        when(clienteRepository.existeCorreo(CORREO)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(DatosDePrueba.solicitudDeAlta()))
                .isInstanceOf(ClienteYaRegistradoException.class);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void registrarRechazaSaldoInicialNegativo() {
        propiedades.setSaldoInicial(new BigDecimal("-1"));
        when(cuentaRepository.siguienteNumeroCuenta()).thenReturn(1000000001L);

        assertThatThrownBy(() -> servicio.registrar(DatosDePrueba.solicitudDeAlta()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(clienteRepository, never()).save(any());
    }

    // -------------------------- Actualización --------------------------

    @Test
    void actualizarCambiaDatosPeroConservaCurpRfcYCuenta() {
        when(clienteRepository.buscarPorId(1)).thenReturn(Optional.of(DatosDePrueba.clienteExistente("hash")));

        ClienteUpdateRequest cambios = new ClienteUpdateRequest();
        DatosDePrueba.llenarDatos(cambios);
        cambios.setNombre("Fernanda");
        cambios.setCorreo("nuevo@correo.com");
        cambios.getDomicilio().setCodigoPostal("37000");

        ClienteResponse respuesta = servicio.actualizar(1, cambios);

        assertThat(respuesta.getNombre()).isEqualTo("Fernanda");
        assertThat(respuesta.getCorreo()).isEqualTo("nuevo@correo.com");
        assertThat(respuesta.getDomicilio().getCodigoPostal()).isEqualTo("37000");
        assertThat(respuesta.getCurp()).isEqualTo(CURP);
        assertThat(respuesta.getRfc()).isEqualTo(RFC);
        assertThat(respuesta.getCuenta().getNumeroCuenta()).isEqualTo(NUMERO_CUENTA);
    }

    @Test
    void actualizarConDatosInvalidosNoConsultaLaBaseDeDatos() {
        ClienteUpdateRequest cambios = new ClienteUpdateRequest();
        DatosDePrueba.llenarDatos(cambios);
        cambios.setFechaNacimiento("12/03/1995");

        assertThatThrownBy(() -> servicio.actualizar(1, cambios)).isInstanceOf(ValidacionException.class);
        verify(clienteRepository, never()).buscarPorId(any());
    }

    @Test
    void actualizarRechazaCorreoDeOtroCliente() {
        when(clienteRepository.buscarPorId(1)).thenReturn(Optional.of(DatosDePrueba.clienteExistente("hash")));
        when(clienteRepository.existeCorreoEnOtroCliente(CORREO, 1)).thenReturn(true);

        ClienteUpdateRequest cambios = new ClienteUpdateRequest();
        DatosDePrueba.llenarDatos(cambios);

        assertThatThrownBy(() -> servicio.actualizar(1, cambios))
                .isInstanceOf(ClienteYaRegistradoException.class);
    }

    @Test
    void actualizarRechazaClienteDadoDeBaja() {
        Cliente inactivo = DatosDePrueba.clienteExistente("hash");
        inactivo.darDeBaja();
        when(clienteRepository.buscarPorId(1)).thenReturn(Optional.of(inactivo));

        ClienteUpdateRequest cambios = new ClienteUpdateRequest();
        DatosDePrueba.llenarDatos(cambios);

        assertThatThrownBy(() -> servicio.actualizar(1, cambios))
                .isInstanceOf(ClienteInactivoException.class);
    }

    // --------------------------- Baja lógica ---------------------------

    @Test
    void darDeBajaDesactivaClienteEInactivaSuCuentaSinBorrarNada() {
        Cliente existente = DatosDePrueba.clienteExistente("hash");
        when(clienteRepository.buscarPorId(1)).thenReturn(Optional.of(existente));

        servicio.darDeBaja(1);

        assertThat(existente.isActivo()).isFalse();
        assertThat(existente.getCuenta().getEstatus()).isEqualTo(EstatusCuenta.INACTIVA);
        verify(clienteRepository, never()).delete(any());
        verify(clienteRepository, never()).deleteById(any());
    }

    // ----------------------------- Consultas -----------------------------

    @Test
    void consultarPorIdInexistenteLanzaClienteNoEncontrado() {
        when(clienteRepository.buscarPorId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultarPorId(99))
                .isInstanceOf(ClienteNoEncontradoException.class);
    }

    @Test
    void consultasRechazanParametrosInvalidos() {
        assertThatThrownBy(() -> servicio.consultarPorId(0)).isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> servicio.consultarPorCurp("ABC")).isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> servicio.consultarPorRfc("ABC")).isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> servicio.consultarPorCorreo("sin-arroba")).isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> servicio.consultarPorNumeroCuenta("123")).isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> servicio.consultarRegistradosEntre("2026-02-01", "2026-01-01"))
                .isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> servicio.consultarRegistradosEntre("01/02/2026", null))
                .isInstanceOf(ValidacionException.class);
    }
}
