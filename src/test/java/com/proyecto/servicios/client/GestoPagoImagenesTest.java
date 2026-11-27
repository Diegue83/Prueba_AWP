package com.proyecto.servicios.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GestoPagoImagenesTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};

    private HttpServer servidor;
    private final AtomicInteger peticiones = new AtomicInteger();
    private final AtomicReference<String> ruta = new AtomicReference<>();

    @BeforeEach
    void preparar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/sistema/images/gestopago/", intercambio -> {
            peticiones.incrementAndGet();
            ruta.set(intercambio.getRequestURI().getPath());
            boolean existe = intercambio.getRequestURI().getPath().endsWith("/79.png");
            if (existe) {
                intercambio.getResponseHeaders().add("Content-Type", "image/png;charset=UTF-8");
                intercambio.sendResponseHeaders(200, PNG.length);
                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.write(PNG);
                }
            } else {
                intercambio.sendResponseHeaders(404, -1);
                intercambio.close();
            }
        });
        servidor.start();
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    @Test
    void descargaElLogoUnaVezYLuegoLoSirveDeMemoria() {
        GestoPagoImagenes imagenes = new GestoPagoImagenes(url() + "/");

        assertThat(imagenes.obtener(GestoPagoImagenes.Tipo.LOGO, 79)).contains(PNG);
        assertThat(imagenes.obtener(GestoPagoImagenes.Tipo.LOGO, 79)).contains(PNG);

        assertThat(ruta.get()).isEqualTo("/sistema/images/gestopago/servicios/79.png");
        assertThat(peticiones.get()).isEqualTo(1);
    }

    @Test
    void unaImagenInexistenteTambienSeRecuerda() {
        GestoPagoImagenes imagenes = new GestoPagoImagenes(url());

        assertThat(imagenes.obtener(GestoPagoImagenes.Tipo.AYUDA_REFERENCIA, 80)).isEmpty();
        assertThat(imagenes.obtener(GestoPagoImagenes.Tipo.AYUDA_REFERENCIA, 80)).isEmpty();

        assertThat(ruta.get()).isEqualTo("/sistema/images/gestopago/referencias/80.jpg");
        assertThat(peticiones.get()).isEqualTo(1);
    }

    @Test
    void unFalloDeRedNoSeGuardaParaPoderReintentar() throws IOException {
        int puerto = servidor.getAddress().getPort();
        servidor.stop(0);
        GestoPagoImagenes imagenes = new GestoPagoImagenes("http://127.0.0.1:" + puerto);

        assertThat(imagenes.obtener(GestoPagoImagenes.Tipo.LOGO, 79)).isEmpty();

        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
        servidor.createContext("/sistema/images/gestopago/", intercambio -> {
            intercambio.getResponseHeaders().add("Content-Type", "image/png");
            intercambio.sendResponseHeaders(200, PNG.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(PNG);
            }
        });
        servidor.start();

        assertThat(imagenes.obtener(GestoPagoImagenes.Tipo.LOGO, 79)).contains(PNG);
    }

    private String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }
}
