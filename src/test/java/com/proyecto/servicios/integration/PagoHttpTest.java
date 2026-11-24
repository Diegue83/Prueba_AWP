package com.proyecto.servicios.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.SeguridadWebConfig;
import com.proyecto.servicios.controller.PagoController;
import com.proyecto.servicios.controller.SegundoFactorController;
import com.proyecto.servicios.enums.EstatusPago;
import com.proyecto.servicios.exception.GestoPagoTimeoutException;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.exception.SegundoFactorInvalidoException;
import com.proyecto.servicios.model.pagos.PagoResponse;
import com.proyecto.servicios.model.pagos.SegundoFactorRegistroResponse;
import com.proyecto.servicios.security.AutenticacionInterceptor;
import com.proyecto.servicios.security.ControlAcceso;
import com.proyecto.servicios.security.TokenService;
import com.proyecto.servicios.service.PagoService;
import com.proyecto.servicios.service.SegundoFactorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = PagoHttpTest.HttpTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "seguridad.jwt.secreto=local-http-tests-only-secret-key-32-chars",
                "seguridad.jwt.minutos-vigencia=5",
                "spring.main.banner-mode=off"
        })
class PagoHttpTest {

    private static final String PAGO_VALIDO = """
            {
              "numeroCuenta": "1000000001",
              "idServicio": 2284,
              "idProducto": 14302,
              "monto": 100.00,
              "telefono": "5512345678",
              "codigo2fa": "123456"
            }
            """;

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            RedisAutoConfiguration.class
    })
    @Import({
            PagoController.class,
            SegundoFactorController.class,
            GlobalExceptionHandler.class,
            SeguridadWebConfig.class,
            AutenticacionInterceptor.class,
            ControlAcceso.class,
            TokenService.class
    })
    static class HttpTestApplication {
    }

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TokenService tokenService;
    @LocalServerPort
    private int port;
    @MockBean
    private PagoService pagoService;
    @MockBean
    private SegundoFactorService segundoFactorService;

    private final HttpClient http = HttpClient.newHttpClient();
    private String token;

    @BeforeEach
    void preparar() {
        token = tokenService.generar(7);
    }

    @Test
    void sinTokenNoSePuedePagar() throws Exception {
        HttpResponse<String> respuesta = post("/pagos", PAGO_VALIDO, null, "clave-0001-abcd");

        assertThat(respuesta.statusCode()).isEqualTo(401);
        verify(pagoService, never()).pagar(any(), any(), any());
    }

    @Test
    void exigeElEncabezadoIdempotencyKey() throws Exception {
        HttpResponse<String> respuesta = post("/pagos", PAGO_VALIDO, token, null);

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(respuesta.body()).contains("Idempotency-Key");
    }

    @Test
    void rechazaCuerposIncompletosOConCamposDeMas() throws Exception {
        HttpResponse<String> incompleto = post("/pagos", """
                {"numeroCuenta":"123","idProducto":-1,"monto":0,"codigo2fa":"12"}
                """, token, "clave-0001-abcd");
        HttpResponse<String> campoExtra = post("/pagos", PAGO_VALIDO.replace("\"codigo2fa\"", "\"saldo\": 9999, \"codigo2fa\""),
                token, "clave-0001-abcd");

        assertThat(incompleto.statusCode()).isEqualTo(400);
        JsonNode cuerpo = objectMapper.readTree(incompleto.body());
        assertThat(cuerpo.path("datos").fieldNames()).toIterable()
                .contains("numeroCuenta", "idServicio", "idProducto", "monto", "codigo2fa");
        assertThat(campoExtra.statusCode()).isEqualTo(400);
        assertThat(campoExtra.body()).contains("saldo");
        verify(pagoService, never()).pagar(any(), any(), any());
    }

    @Test
    void elEstatusHttpReflejaElResultadoDelPago() throws Exception {
        when(pagoService.pagar(eq(7), eq("clave-aplicado"), any())).thenReturn(respuesta(EstatusPago.APLICADO));
        when(pagoService.pagar(eq(7), eq("clave-rechazo"), any())).thenReturn(respuesta(EstatusPago.RECHAZADO));
        when(pagoService.pagar(eq(7), eq("clave-pendiente"), any())).thenReturn(respuesta(EstatusPago.EN_CONCILIACION));

        HttpResponse<String> aplicado = post("/pagos", PAGO_VALIDO, token, "clave-aplicado");

        assertThat(aplicado.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(aplicado.body()).path("datos").path("numAutorizacion").asText()).isEqualTo("9001");
        assertThat(post("/pagos", PAGO_VALIDO, token, "clave-rechazo").statusCode()).isEqualTo(422);
        assertThat(post("/pagos", PAGO_VALIDO, token, "clave-pendiente").statusCode()).isEqualTo(202);
    }

    @Test
    void losErroresDeNegocioYDelProveedorTienenSuPropioEstatus() throws Exception {
        when(pagoService.pagar(eq(7), eq("clave-saldo"), any())).thenThrow(new SaldoInsuficienteException());
        when(pagoService.pagar(eq(7), eq("clave-2fa"), any())).thenThrow(new SegundoFactorInvalidoException());
        when(pagoService.pagar(eq(7), eq("clave-timeout"), any())).thenThrow(new GestoPagoTimeoutException("sendTx"));
        when(pagoService.pagar(eq(7), eq("clave-bloqueo"), any()))
                .thenThrow(new PessimisticLockingFailureException("lock timeout"));

        HttpResponse<String> saldo = post("/pagos", PAGO_VALIDO, token, "clave-saldo");
        HttpResponse<String> segundoFactor = post("/pagos", PAGO_VALIDO, token, "clave-2fa");
        HttpResponse<String> timeout = post("/pagos", PAGO_VALIDO, token, "clave-timeout");
        HttpResponse<String> bloqueo = post("/pagos", PAGO_VALIDO, token, "clave-bloqueo");

        assertThat(saldo.statusCode()).isEqualTo(409);
        assertThat(saldo.body()).contains("SALDO_INSUFICIENTE");
        assertThat(segundoFactor.statusCode()).isEqualTo(401);
        assertThat(segundoFactor.body()).contains("CODIGO_2FA_INVALIDO").doesNotContain("123456");
        assertThat(timeout.statusCode()).isEqualTo(504);
        assertThat(bloqueo.statusCode()).isEqualTo(409);
        assertThat(bloqueo.body()).contains("OPERACION_EN_CURSO").doesNotContain("lock timeout");
    }

    @Test
    void elRegistro2faNoSeGuardaEnCache() throws Exception {
        when(segundoFactorService.registrar(7))
                .thenReturn(new SegundoFactorRegistroResponse("JBSWY3DPEHPK3PXP", "otpauth://totp/x"));

        HttpResponse<String> respuesta = send(HttpRequest.newBuilder(URI.create(url("/2fa/registro")))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.headers().firstValue(HttpHeaders.CACHE_CONTROL)).hasValue("no-store");
    }

    @Test
    void consultaElPagoDelClienteAutenticado() throws Exception {
        when(pagoService.consultar(7, 10L)).thenReturn(respuesta(EstatusPago.EN_CONCILIACION));

        HttpResponse<String> respuesta = send(HttpRequest.newBuilder(URI.create(url("/pagos/10")))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .GET()
                .build());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("EN_CONCILIACION");
    }

    private PagoResponse respuesta(EstatusPago estatus) {
        return new PagoResponse(10L, estatus, 14302, new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"),
                estatus == EstatusPago.APLICADO ? "9001" : null, "01", "mensaje", LocalDateTime.now());
    }

    private HttpResponse<String> post(String ruta, String json, String bearer, String claveIdempotencia)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url(ruta)))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (bearer != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
        }
        if (claveIdempotencia != null) {
            request.header(PagoController.IDEMPOTENCY_KEY, claveIdempotencia);
        }
        return send(request.POST(HttpRequest.BodyPublishers.ofString(json)).build());
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String url(String ruta) {
        return "http://127.0.0.1:" + port + ruta;
    }
}
