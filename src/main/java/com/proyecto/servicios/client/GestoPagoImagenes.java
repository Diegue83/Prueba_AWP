package com.proyecto.servicios.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PuntoRed publica el logotipo y la imagen de ayuda de cada servicio, pero sin encabezados CORS,
 * así que el front web no puede descargarlas directo. Se sirven desde aquí y se guardan en memoria;
 * una imagen inexistente también se recuerda para no volver a pedirla.
 */
@Service
@Slf4j
public class GestoPagoImagenes {

    public enum Tipo {
        LOGO("servicios", ".png", "image/png", 1000),
        AYUDA_REFERENCIA("referencias", ".jpg", "image/jpeg", 40);

        private final String carpeta;
        private final String extension;
        private final String contentType;
        private final int maximoEnCache;

        Tipo(String carpeta, String extension, String contentType, int maximoEnCache) {
            this.carpeta = carpeta;
            this.extension = extension;
            this.contentType = contentType;
            this.maximoEnCache = maximoEnCache;
        }

        public String contentType() {
            return contentType;
        }
    }

    private static final int MAXIMO_BYTES = 3 * 1024 * 1024;

    private final HttpClient httpClient;
    private final String urlBase;
    private final Map<Tipo, Map<Integer, Optional<byte[]>>> cache = Map.of(
            Tipo.LOGO, new ConcurrentHashMap<>(),
            Tipo.AYUDA_REFERENCIA, new ConcurrentHashMap<>());

    public GestoPagoImagenes(@Value("${gestopago.imagenes.url}") String urlBase) {
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public Optional<byte[]> obtener(Tipo tipo, int idServicio) {
        Map<Integer, Optional<byte[]>> enMemoria = cache.get(tipo);
        Optional<byte[]> guardada = enMemoria.get(idServicio);
        if (guardada != null) {
            return guardada;
        }
        Descarga descarga = descargar(tipo, idServicio);
        Optional<byte[]> imagen = Optional.ofNullable(descarga.bytes());
        if (descarga.concluyente()) {
            if (enMemoria.size() >= tipo.maximoEnCache) {
                enMemoria.clear();
            }
            enMemoria.put(idServicio, imagen);
        }
        return imagen;
    }

    /** concluyente=false cuando falló la red: no se guarda en memoria para reintentar después. */
    private record Descarga(boolean concluyente, byte[] bytes) {
    }

    private Descarga descargar(Tipo tipo, int idServicio) {
        URI uri = URI.create(urlBase + "/sistema/images/gestopago/" + tipo.carpeta + "/" + idServicio + tipo.extension);
        HttpRequest peticion = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build();
        try {
            HttpResponse<byte[]> respuesta = httpClient.send(peticion, HttpResponse.BodyHandlers.ofByteArray());
            String contentType = respuesta.headers().firstValue("Content-Type").orElse("");
            boolean esImagen = respuesta.statusCode() == 200 && contentType.startsWith("image/")
                    && respuesta.body().length > 0 && respuesta.body().length <= MAXIMO_BYTES;
            return new Descarga(true, esImagen ? respuesta.body() : null);
        } catch (IOException e) {
            log.warn("No se pudo descargar la imagen {} del servicio {} ({})", tipo, idServicio,
                    e.getClass().getSimpleName());
            return new Descarga(false, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Descarga(false, null);
        }
    }
}
