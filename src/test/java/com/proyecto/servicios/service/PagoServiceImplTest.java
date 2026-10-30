package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoCifrador;
import com.proyecto.servicios.client.GestoPagoTransaccionesIntegration;
import com.proyecto.servicios.config.GestoPagoTransaccionesProperties;
import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.pagos.Pago;
import com.proyecto.servicios.enums.EstatusPago;
import com.proyecto.servicios.exception.ConfiguracionIncompletaException;
import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.exception.IdempotenciaConflictoException;
import com.proyecto.servicios.exception.SegundoFactorInvalidoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado.Tipo;
import com.proyecto.servicios.model.pagos.PagoRequest;
import com.proyecto.servicios.model.pagos.PagoResponse;
import com.proyecto.servicios.model.pagos.PagoValidado;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.pagos.PagoRepository;
import com.proyecto.servicios.service.Impl.PagoServiceImpl;
import com.proyecto.servicios.service.Impl.PagoTransacciones;
import com.proyecto.servicios.service.Impl.PagoValidador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoServiceImplTest {

    private static final int CLIENTE = 1;
    private static final String CLAVE = "pago-0001-abcdef";
    private static final long ID_PAGO = 10L;

    @Mock
    private PagoValidador validador;
    @Mock
    private SegundoFactorService segundoFactorService;
    @Mock
    private PagoTransacciones transacciones;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private GestoPagoTransaccionesIntegration integracion;
    @Mock
    private GestoPagoCifrador cifrador;
    @Mock
    private GestoPagoTokenService tokenService;

    private final GestoPagoTransaccionesProperties transaccionesProperties = new GestoPagoTransaccionesProperties();
    private PagoServiceImpl service;
    private Pago pago;

    @BeforeEach
    void preparar() {
        service = new PagoServiceImpl(validador, segundoFactorService, transacciones, pagoRepository,
                cuentaRepository, integracion, cifrador, tokenService, new PagosProperties(), transaccionesProperties);
        transaccionesProperties.setEsperaConfirmacionSegundos(0);
        pago = pago(EstatusPago.PENDIENTE);
        when(validador.validar(any())).thenAnswer(invocacion -> {
            PagoRequest request = invocacion.getArgument(0);
            return new PagoValidado(request.getIdProducto(), request.getIdServicio(), 1, request.getMonto(),
                    BigDecimal.ZERO, request.getTelefono(), request.getReferencia());
        });
        when(pagoRepository.findByClienteIdAndClaveIdempotencia(CLIENTE, CLAVE)).thenReturn(Optional.empty());
        when(transacciones.registrarYRetener(eq(CLIENTE), anyString(), any(), eq(CLAVE), anyString(), anyString()))
                .thenReturn(pago);
        when(tokenService.obtenerTokenVigente()).thenReturn(token("token-guardado"));
        when(transacciones.aplicar(eq(ID_PAGO), any())).thenReturn(pago(EstatusPago.APLICADO));
        when(transacciones.rechazarYReembolsar(eq(ID_PAGO), any())).thenReturn(pago(EstatusPago.RECHAZADO));
        when(transacciones.enviarAConciliacion(ID_PAGO)).thenReturn(pago(EstatusPago.EN_CONCILIACION));
    }

    @Test
    void pagoExitosoValidaTodoAntesDeRetenerYAplicaConLaRespuestaDelProveedor() {
        when(integracion.enviar(any(), eq("token-guardado"))).thenReturn(resultado(Tipo.EXITOSO, "01"));

        PagoResponse respuesta = service.pagar(CLIENTE, CLAVE, solicitud());

        assertThat(respuesta.estatus()).isEqualTo(EstatusPago.APLICADO);
        var orden = inOrder(validador, cifrador, segundoFactorService, transacciones);
        orden.verify(validador).validar(any());
        orden.verify(cifrador).validarConfiguracion();
        orden.verify(segundoFactorService).verificar(CLIENTE, "123456");
        orden.verify(transacciones).registrarYRetener(eq(CLIENTE), eq("1000000001"), any(), eq(CLAVE), anyString(), anyString());
        orden.verify(transacciones).aplicar(eq(ID_PAGO), any());
        verify(integracion, never()).confirmar(any(), anyString());
    }

    @Test
    void elRechazoDefinitivoDelProveedorDevuelveElDinero() {
        when(integracion.enviar(any(), anyString())).thenReturn(resultado(Tipo.RECHAZADO, "06"));

        PagoResponse respuesta = service.pagar(CLIENTE, CLAVE, solicitud());

        assertThat(respuesta.estatus()).isEqualTo(EstatusPago.RECHAZADO);
        verify(transacciones).rechazarYReembolsar(eq(ID_PAGO), any());
        verify(integracion, never()).confirmar(any(), anyString());
    }

    @Test
    void anteUnTimeoutConsultaConfirmTxConElMismoPayloadYNoReembolsaSiSeAplico() {
        when(integracion.enviar(any(), anyString())).thenReturn(GestoPagoTxResultado.indeterminado("timeout"));
        when(integracion.confirmar(any(), anyString())).thenReturn(resultado(Tipo.EXITOSO, "01"));

        PagoResponse respuesta = service.pagar(CLIENTE, CLAVE, solicitud());

        assertThat(respuesta.estatus()).isEqualTo(EstatusPago.APLICADO);
        ArgumentCaptor<GestoPagoTxPayload> enviado = ArgumentCaptor.forClass(GestoPagoTxPayload.class);
        ArgumentCaptor<GestoPagoTxPayload> confirmado = ArgumentCaptor.forClass(GestoPagoTxPayload.class);
        verify(integracion).enviar(enviado.capture(), anyString());
        verify(integracion).confirmar(confirmado.capture(), anyString());
        assertThat(confirmado.getValue()).isEqualTo(enviado.getValue());
        assertThat(confirmado.getValue().upc()).isEqualTo("UPC000000000001");
        verify(transacciones, never()).rechazarYReembolsar(any(), any());
    }

    @Test
    void siConfirmTxTampocoEsConcluyenteElPagoQuedaEnConciliacionConElDineroRetenido() {
        when(integracion.enviar(any(), anyString())).thenReturn(GestoPagoTxResultado.indeterminado("timeout"));
        when(integracion.confirmar(any(), anyString())).thenReturn(GestoPagoTxResultado.indeterminado("timeout"));

        PagoResponse respuesta = service.pagar(CLIENTE, CLAVE, solicitud());

        assertThat(respuesta.estatus()).isEqualTo(EstatusPago.EN_CONCILIACION);
        verify(transacciones, never()).rechazarYReembolsar(any(), any());
    }

    @Test
    void soloElCodigo70DeConfirmTxPermiteDevolverElDinero() {
        when(integracion.enviar(any(), anyString())).thenReturn(GestoPagoTxResultado.indeterminado("timeout"));
        when(integracion.confirmar(any(), anyString()))
                .thenReturn(resultado(Tipo.RECHAZADO, "51"), resultado(Tipo.RECHAZADO, "70"));

        assertThat(service.pagar(CLIENTE, CLAVE, solicitud()).estatus()).isEqualTo(EstatusPago.EN_CONCILIACION);
        verify(transacciones, never()).rechazarYReembolsar(any(), any());

        assertThat(service.pagar(CLIENTE, CLAVE, solicitud()).estatus()).isEqualTo(EstatusPago.RECHAZADO);
    }

    @Test
    void noConsultaConfirmTxAntesDe62SegundosDelEnvio() {
        transaccionesProperties.setEsperaConfirmacionSegundos(62);
        when(integracion.enviar(any(), anyString())).thenReturn(resultado(Tipo.INDETERMINADO, "82"));

        assertThat(service.pagar(CLIENTE, CLAVE, solicitud()).estatus()).isEqualTo(EstatusPago.EN_CONCILIACION);
        verify(integracion, never()).confirmar(any(), anyString());
        verify(transacciones, never()).rechazarYReembolsar(any(), any());
    }

    @Test
    void laConciliacionNoConsultaConfirmTxDespuesDe24Horas() {
        Pago viejo = pago(EstatusPago.EN_CONCILIACION);
        ReflectionTestUtils.setField(viejo, "fechaCreacion", LocalDateTime.now().minusHours(25));
        when(pagoRepository.buscarPorConciliar(any(), any(), any())).thenReturn(List.of(ID_PAGO));
        when(pagoRepository.findById(ID_PAGO)).thenReturn(Optional.of(viejo));

        service.conciliarPendientes();

        verify(transacciones).marcarRevisionManual(ID_PAGO);
        verify(integracion, never()).confirmar(any(), anyString());
    }

    @Test
    void enPagosDeServicioEnviaReferenciaYMontoPagoYEnRecargasNo() {
        Pago servicio = Pago.builder().clienteId(CLIENTE).cuentaId(5).idProducto(205).idServicio(76).tipoFront(2)
                .monto(new BigDecimal("601.99")).comision(new BigDecimal("10.00")).claveIdempotencia(CLAVE)
                .upc("UPC000000000002").horaLocal("20261030101500").telefono("1111111111").referencia("25720381")
                .build();
        ReflectionTestUtils.setField(servicio, "id", ID_PAGO);
        when(transacciones.registrarYRetener(eq(CLIENTE), anyString(), any(), eq(CLAVE), anyString(), anyString()))
                .thenReturn(servicio, pago);
        when(integracion.enviar(any(), anyString())).thenReturn(resultado(Tipo.EXITOSO, "01"));

        service.pagar(CLIENTE, CLAVE, solicitud());
        service.pagar(CLIENTE, CLAVE, solicitud());

        ArgumentCaptor<GestoPagoTxPayload> enviados = ArgumentCaptor.forClass(GestoPagoTxPayload.class);
        verify(integracion, times(2)).enviar(enviados.capture(), anyString());
        GestoPagoTxPayload pagoServicio = enviados.getAllValues().get(0);
        GestoPagoTxPayload recarga = enviados.getAllValues().get(1);
        assertThat(pagoServicio.montoPago()).isEqualByComparingTo("601.99");
        assertThat(pagoServicio.referencia()).isEqualTo("25720381");
        assertThat(pagoServicio.telefono()).isEqualTo("1111111111");
        assertThat(recarga.montoPago()).isNull();
        assertThat(recarga.referencia()).isNull();
    }

    @Test
    void conTokenVencidoLoRenuevaYReintentaUnaVez() {
        when(tokenService.solicitarNuevoToken()).thenReturn(token("token-nuevo"));
        when(integracion.enviar(any(), eq("token-guardado"))).thenThrow(new GestoPagoAutenticacionException("sendTx"));
        when(integracion.enviar(any(), eq("token-nuevo"))).thenReturn(resultado(Tipo.EXITOSO, "01"));

        assertThat(service.pagar(CLIENTE, CLAVE, solicitud()).estatus()).isEqualTo(EstatusPago.APLICADO);
    }

    @Test
    void siElProveedorRechazaTambienElTokenNuevoReembolsaYReportaElError() {
        when(tokenService.solicitarNuevoToken()).thenReturn(token("token-nuevo"));
        when(integracion.enviar(any(), anyString())).thenThrow(new GestoPagoAutenticacionException("sendTx"));

        assertThatThrownBy(() -> service.pagar(CLIENTE, CLAVE, solicitud()))
                .isInstanceOf(GestoPagoAutenticacionException.class);
        verify(transacciones).rechazarYReembolsar(eq(ID_PAGO), any());
    }

    @Test
    void laMismaClaveDeIdempotenciaDevuelveElResultadoAnteriorSinVolverACobrar() {
        Pago aplicado = pago(EstatusPago.APLICADO);
        when(pagoRepository.findByClienteIdAndClaveIdempotencia(CLIENTE, CLAVE)).thenReturn(Optional.of(aplicado));
        registrarCuenta(5);

        PagoResponse respuesta = service.pagar(CLIENTE, CLAVE, solicitud());

        assertThat(respuesta.estatus()).isEqualTo(EstatusPago.APLICADO);
        assertThat(respuesta.idPago()).isEqualTo(ID_PAGO);
        verifyNoInteractions(segundoFactorService, integracion, transacciones);
    }

    @Test
    void laMismaClaveConOtrosDatosSeRechaza() {
        when(pagoRepository.findByClienteIdAndClaveIdempotencia(CLIENTE, CLAVE))
                .thenReturn(Optional.of(pago(EstatusPago.APLICADO)));
        registrarCuenta(5);
        PagoRequest otroMonto = solicitud();
        otroMonto.setMonto(new BigDecimal("150.00"));

        assertThatThrownBy(() -> service.pagar(CLIENTE, CLAVE, otroMonto))
                .isInstanceOf(IdempotenciaConflictoException.class);
        verifyNoInteractions(transacciones, integracion);
    }

    @Test
    void siDosPeticionesConLaMismaClaveLleganJuntasLaSegundaRecibeElPagoDeLaPrimera() {
        when(transacciones.registrarYRetener(eq(CLIENTE), anyString(), any(), eq(CLAVE), anyString(), anyString()))
                .thenThrow(new DataIntegrityViolationException("uq_pagos_idempotencia"));
        when(pagoRepository.findByClienteIdAndClaveIdempotencia(CLIENTE, CLAVE))
                .thenReturn(Optional.empty(), Optional.of(pago(EstatusPago.PENDIENTE)));
        registrarCuenta(5);

        PagoResponse respuesta = service.pagar(CLIENTE, CLAVE, solicitud());

        assertThat(respuesta.idPago()).isEqualTo(ID_PAGO);
        verifyNoInteractions(integracion);
    }

    @Test
    void unCodigo2faInvalidoNoRetieneSaldo() {
        doThrow(new SegundoFactorInvalidoException()).when(segundoFactorService).verificar(CLIENTE, "123456");

        assertThatThrownBy(() -> service.pagar(CLIENTE, CLAVE, solicitud()))
                .isInstanceOf(SegundoFactorInvalidoException.class);
        verifyNoInteractions(transacciones, integracion);
    }

    @Test
    void sinLlaveDeCifradoNoRetieneSaldo() {
        doThrow(new ConfiguracionIncompletaException("pagos")).when(cifrador).validarConfiguracion();

        assertThatThrownBy(() -> service.pagar(CLIENTE, CLAVE, solicitud()))
                .isInstanceOf(ConfiguracionIncompletaException.class);
        verifyNoInteractions(transacciones, integracion, segundoFactorService);
    }

    @Test
    void rechazaClavesDeIdempotenciaInvalidas() {
        assertThatThrownBy(() -> service.pagar(CLIENTE, "corta", solicitud()))
                .isInstanceOf(ValidacionException.class);
        assertThatThrownBy(() -> service.pagar(CLIENTE, null, solicitud()))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void laConciliacionAplicaLosConfirmadosYCuentaIntentosDeLosQueSiguenSinRespuesta() {
        Pago otro = pago(EstatusPago.EN_CONCILIACION);
        ReflectionTestUtils.setField(otro, "id", 11L);
        when(pagoRepository.buscarPorConciliar(any(), any(), any())).thenReturn(List.of(ID_PAGO, 11L));
        when(pagoRepository.findById(ID_PAGO)).thenReturn(Optional.of(pago(EstatusPago.EN_CONCILIACION)));
        when(pagoRepository.findById(11L)).thenReturn(Optional.of(otro));
        when(integracion.confirmar(any(), anyString()))
                .thenReturn(resultado(Tipo.EXITOSO, "01"), GestoPagoTxResultado.indeterminado("timeout"));

        assertThat(service.conciliarPendientes()).isEqualTo(2);

        verify(transacciones).aplicar(eq(ID_PAGO), any());
        verify(transacciones).registrarIntentoConciliacion(eq(11L), anyInt());
        verify(transacciones, never()).rechazarYReembolsar(any(), any());
    }

    private PagoRequest solicitud() {
        return PagoValidadorTest.solicitud(2284, 14302, "100.00", "5512345678", null);
    }

    private Pago pago(EstatusPago estatus) {
        Pago nuevo = Pago.builder()
                .clienteId(CLIENTE)
                .cuentaId(5)
                .idProducto(14302)
                .idServicio(2284)
                .tipoFront(1)
                .monto(new BigDecimal("100.00"))
                .comision(BigDecimal.ZERO)
                .claveIdempotencia(CLAVE)
                .upc("UPC000000000001")
                .horaLocal("20261030101500")
                .telefono("5512345678")
                .build();
        ReflectionTestUtils.setField(nuevo, "id", ID_PAGO);
        ReflectionTestUtils.setField(nuevo, "estatus", estatus);
        ReflectionTestUtils.setField(nuevo, "fechaCreacion", LocalDateTime.now());
        return nuevo;
    }

    private void registrarCuenta(int id) {
        Cuenta cuenta = mock(Cuenta.class);
        when(cuenta.getId()).thenReturn(id);
        when(cuentaRepository.buscarPorNumero("1000000001")).thenReturn(Optional.of(cuenta));
    }

    private GestoPagoTxResultado resultado(Tipo tipo, String codigo) {
        return new GestoPagoTxResultado(tipo, codigo, "texto", tipo == Tipo.EXITOSO ? "9001" : "-1", "55");
    }

    private GestoPagoToken token(String valor) {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken(valor);
        token.setActivo(true);
        return token;
    }
}
