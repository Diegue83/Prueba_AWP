package com.proyecto.servicios.client;

import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.exception.GestoPagoComunicacionException;
import com.proyecto.servicios.exception.GestoPagoRespuestaException;
import com.proyecto.servicios.exception.GestoPagoTimeoutException;
import com.sun.net.httpserver.HttpServer;
import feign.Feign;
import feign.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestoPagoProductsIntegrationTest {

    private static final String RUTA = "/sistema/service/getProductList.do";
    private static final String XML = "<RESPONSE><MENSAJE><CODIGO>01</CODIGO></MENSAJE></RESPONSE>";

    private HttpServer servidor;
    private final AtomicReference<String> authorizationRecibido = new AtomicReference<>();
    private volatile int estatus = 200;
    private volatile long demoraMs;

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext(RUTA, intercambio -> {
            authorizationRecibido.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            dormir(demoraMs);
            byte[] cuerpo = (estatus == 200 ? XML : "{\"success\":false}").getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "text/xml;charset=UTF-8");
            intercambio.sendResponseHeaders(estatus, cuerpo.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    @Test
    void enviaElTokenComoBearerYDevuelveElXml() {
        String xml = integracion(urlServidor()).consultarCatalogoXml("token-de-prueba");

        assertThat(xml).isEqualTo(XML);
        assertThat(authorizationRecibido.get()).isEqualTo("Bearer token-de-prueba");
    }

    @Test
    void traduceElRechazoDelTokenAErrorDeAutenticacion() {
        estatus = HttpStatus.UNAUTHORIZED.value();

        assertThatThrownBy(() -> integracion(urlServidor()).consultarCatalogoXml("token-vencido"))
                .isInstanceOf(GestoPagoAutenticacionException.class)
                .hasMessageNotContaining("token-vencido");
    }

    @Test
    void traduceUnaRespuestaHttpNoExitosa() {
        estatus = HttpStatus.SERVICE_UNAVAILABLE.value();

        assertThatThrownBy(() -> integracion(urlServidor()).consultarCatalogoXml("token"))
                .isInstanceOf(GestoPagoRespuestaException.class)
                .hasMessageContaining("HTTP 503");
    }

    @Test
    void traduceElTiempoDeEsperaAgotado() {
        demoraMs = 1_500;

        assertThatThrownBy(() -> integracion(urlServidor()).consultarCatalogoXml("token"))
                .isInstanceOf(GestoPagoTimeoutException.class);
    }

    @Test
    void traduceLaFaltaDeConexion() throws IOException {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }

        assertThatThrownBy(() -> integracion("http://127.0.0.1:" + puertoLibre).consultarCatalogoXml("token"))
                .isInstanceOf(GestoPagoComunicacionException.class);
    }

    private GestoPagoProductsIntegration integracion(String url) {
        GestoPagoProductsClient client = Feign.builder()
                .contract(new SpringMvcContract())
                .options(new Request.Options(500, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS, true))
                .target(GestoPagoProductsClient.class, url);
        return new GestoPagoProductsIntegration(client);
    }

    private String urlServidor() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
