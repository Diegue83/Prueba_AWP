package com.proyecto.servicios.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GestoPagoTransaccionesProperties;
import com.proyecto.servicios.exception.ConfiguracionIncompletaException;
import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado.Tipo;
import com.sun.net.httpserver.HttpServer;
import feign.Feign;
import feign.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import feign.form.spring.SpringFormEncoder;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GestoPagoTransaccionesIntegrationTest {

    private static final String XML_EXITO = "<RESPONSE><ID_TX>55</ID_TX><NUM_AUTORIZACION>9001</NUM_AUTORIZACION>"
            + "<MENSAJE><CODIGO>01</CODIGO><TEXTO>Exito</TEXTO></MENSAJE></RESPONSE>";

    private final GestoPagoTransaccionesProperties properties = new GestoPagoTransaccionesProperties();
    private final GestoPagoCifrador cifrador = new GestoPagoCifrador(properties);
    private final GestoPagoBitacoraService bitacora = mock(GestoPagoBitacoraService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> rutaRecibida = new AtomicReference<>();
    private final AtomicReference<String> authorizationRecibido = new AtomicReference<>();
    private final AtomicReference<Map<String, String>> formularioRecibido = new AtomicReference<>();

    private HttpServer servidor;
    private volatile int estatus = 200;
    private volatile String cuerpo = XML_EXITO;
    private volatile long demoraMs;

    @BeforeEach
    void preparar() throws IOException {
        properties.setLlaveAes(Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes()));
        properties.setIvAes(Base64.getEncoder().encodeToString("abcdef9876543210".getBytes()));
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/sistema/service/", intercambio -> {
            rutaRecibida.set(intercambio.getRequestURI().getPath());
            authorizationRecibido.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            formularioRecibido.set(leerFormulario(new String(intercambio.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8)));
            dormir(demoraMs);
            byte[] respuesta = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "text/xml;charset=UTF-8");
            intercambio.sendResponseHeaders(estatus, respuesta.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(respuesta);
            }
        });
        servidor.start();
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    @Test
    void enviaElPayloadCifradoEnSignedConRandomYTokenBearer() throws Exception {
        GestoPagoTxResultado resultado = integracion(url()).enviar(payload(), "token-x");

        assertThat(resultado.tipo()).isEqualTo(Tipo.EXITOSO);
        assertThat(resultado.numAutorizacion()).isEqualTo("9001");
        assertThat(rutaRecibida.get()).isEqualTo("/sistema/service/sendTx.do");
        assertThat(authorizationRecibido.get()).isEqualTo("Bearer token-x");
        Map<String, String> formulario = formularioRecibido.get();
        assertThat(formulario).containsOnlyKeys("random", "signed");
        assertThat(formulario.get("random")).matches("\\d+");
        assertThat(formulario.get("signed")).doesNotContain("5512345678");

        JsonNode json = objectMapper.readTree(cifrador.descifrar(formulario.get("signed")));
        assertThat(json.get("idServicio").asInt()).isEqualTo(2284);
        assertThat(json.get("idProducto").asInt()).isEqualTo(14302);
        assertThat(json.get("montoPago").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(json.get("telefono").asText()).isEqualTo("5512345678");
        assertThat(json.get("horaLocal").asText()).isEqualTo("20261030101500");
        assertThat(json.get("upc").asText()).isEqualTo("ABC123DEF456GHI");
        assertThat(json.has("referencia")).isFalse();
        verify(bitacora).registrar(eq("sendTx"), eq("ABC123DEF456GHI"), contains("\"telefono\":\"5512345678\""),
                eq(200), contains("<CODIGO>01</CODIGO>"), anyLong());
    }

    @Test
    void confirmTxUsaLaMismaRutaDelProveedorYElMismoPayload() throws Exception {
        integracion(url()).confirmar(payload(), "token-x");

        assertThat(rutaRecibida.get()).isEqualTo("/sistema/service/confirmTx.do");
        JsonNode json = objectMapper.readTree(cifrador.descifrar(formularioRecibido.get().get("signed")));
        assertThat(json.get("upc").asText()).isEqualTo("ABC123DEF456GHI");
    }

    @Test
    void unTimeoutNoSeTomaComoRechazo() {
        demoraMs = 1_500;

        assertThat(integracion(url()).enviar(payload(), "token").tipo()).isEqualTo(Tipo.INDETERMINADO);
        verify(bitacora).registrar(eq("sendTx"), eq("ABC123DEF456GHI"), contains("ABC123DEF456GHI"),
                isNull(), eq("RetryableException"), anyLong());
    }

    @Test
    void unErrorDelServidorDelProveedorEsIndeterminado() {
        estatus = 503;
        cuerpo = "Service Unavailable";

        assertThat(integracion(url()).enviar(payload(), "token").tipo()).isEqualTo(Tipo.INDETERMINADO);
    }

    @Test
    void sinConexionElResultadoEsIndeterminado() throws IOException {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }

        assertThat(integracion("http://127.0.0.1:" + puertoLibre).enviar(payload(), "token").tipo())
                .isEqualTo(Tipo.INDETERMINADO);
    }

    @Test
    void unTokenRechazadoSeReportaParaRenovarlo() {
        estatus = 401;
        cuerpo = "{\"success\":false, \"token\":\"INVALID\", \"message\":\"Invalid token\"}";

        assertThatThrownBy(() -> integracion(url()).enviar(payload(), "token-vencido"))
                .isInstanceOf(GestoPagoAutenticacionException.class)
                .hasMessageNotContaining("token-vencido");
    }

    @Test
    void unaPeticionMalFormadaEsRechazoDefinitivo() {
        estatus = 400;
        cuerpo = "bad request";

        GestoPagoTxResultado resultado = integracion(url()).enviar(payload(), "token");

        assertThat(resultado.tipo()).isEqualTo(Tipo.RECHAZADO);
        assertThat(resultado.codigo()).isEqualTo("HTTP_400");
    }

    @Test
    void sinLlaveAesNoSeEnviaNada() {
        properties.setLlaveAes("");

        assertThatThrownBy(() -> integracion(url()).enviar(payload(), "token"))
                .isInstanceOf(ConfiguracionIncompletaException.class);
        assertThat(rutaRecibida.get()).isNull();
    }

    private GestoPagoTransaccionesIntegration integracion(String url) {
        GestoPagoTransaccionesClient client = Feign.builder()
                .contract(new SpringMvcContract())
                .encoder(new SpringFormEncoder())
                .options(new Request.Options(500, TimeUnit.MILLISECONDS, 1_000, TimeUnit.MILLISECONDS, true))
                .target(GestoPagoTransaccionesClient.class, url);
        return new GestoPagoTransaccionesIntegration(client, cifrador,
                new GestoPagoTxXmlParser(properties), objectMapper, bitacora);
    }

    private GestoPagoTxPayload payload() {
        return new GestoPagoTxPayload(2284, 14302, null, new BigDecimal("100.00"), "5512345678",
                "20261030101500", "ABC123DEF456GHI", "083");
    }

    private String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    private static Map<String, String> leerFormulario(String cuerpo) {
        return Arrays.stream(cuerpo.split("&"))
                .filter(par -> par.contains("="))
                .map(par -> par.split("=", 2))
                .collect(Collectors.toMap(
                        par -> URLDecoder.decode(par[0], StandardCharsets.UTF_8),
                        par -> URLDecoder.decode(par[1], StandardCharsets.UTF_8)));
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
