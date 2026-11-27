package com.proyecto.servicios.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoProductsIntegration;
import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.exception.GestoPagoIntegracionException;
import com.proyecto.servicios.exception.GestoPagoRespuestaException;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import com.proyecto.servicios.service.GestoPagoProductService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class GestoPagoProductServiceImpl implements GestoPagoProductService {

    private static final String CACHE_KEY_PREFIX = "gestopago:productos:";
    private static final String OPERACION_CATALOGO = "getProductList";

    private final GestoPagoProductsIntegration productsIntegration;
    private final GestoPagoTokenService tokenService;
    private final GestoPagoProductPersistenceService persistenceService;
    private final GestoPagoProductXmlParser xmlParser;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final JavaType tipoCatalogo;
    private final String cacheKey;
    private final Duration cacheTtl;
    private final String intentoKey;
    private final String respaldoKey;
    private final ReentrantLock candadoDescarga = new ReentrantLock();
    private volatile Instant ultimoIntentoDescarga;

    @Value("${gestopago.productos.intervalo-descarga-minutos:480}")
    private long intervaloDescargaMinutos;

    public GestoPagoProductServiceImpl(
            GestoPagoProductsIntegration productsIntegration,
            GestoPagoTokenService tokenService,
            GestoPagoProductPersistenceService persistenceService,
            GestoPagoProductXmlParser xmlParser,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
            @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo,
            @Value("${gestopago.productos.cache-ttl-seconds:3600}") long cacheTtlSeconds) {
        this.productsIntegration = productsIntegration;
        this.tokenService = tokenService;
        this.persistenceService = persistenceService;
        this.xmlParser = xmlParser;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.tipoCatalogo = objectMapper.getTypeFactory().constructCollectionType(List.class, GestoPagoProducto.class);
        this.cacheKey = CACHE_KEY_PREFIX + idDistribuidor + ":" + codigoDispositivo;
        this.intentoKey = cacheKey + ":ultimo-intento-descarga";
        this.respaldoKey = cacheKey + ":respaldo";
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
    }

    @Override
    public List<GestoPagoProducto> consultarProductos() {
        Optional<List<GestoPagoProducto>> enCache = leerCache();
        if (enCache.isPresent()) {
            return enCache.get();
        }

        List<GestoPagoProducto> productos = persistenceService.consultarCatalogo();
        if (productos.isEmpty()) {
            return descargarSiFalta();
        }
        guardarEnCache(productos);
        return productos;
    }

    @Override
    public List<GestoPagoProducto> sincronizarProductos() {
        List<GestoPagoProducto> productos = descargarYGuardar();
        log.info("Catálogo de GestoPago sincronizado: {} productos", productos.size());
        return productos;
    }

    /**
     * Al reiniciar no se vuelve a descargar ni a escribir el catálogo si ya está en PostgreSQL;
     * solo se carga a Redis cuando ahí no existe. Con la base vacía (por ejemplo, una base nueva)
     * primero se restaura el respaldo de Redis y solo si no hay respaldo se llama a GestoPago.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void cargarCatalogoAlIniciar() {
        try {
            if (!persistenceService.existeCatalogo()) {
                if (restaurarDesdeRespaldo().isEmpty()) {
                    sincronizarProductos();
                }
                return;
            }
            Optional<List<GestoPagoProducto>> enCache = leerCache();
            if (enCache.isEmpty()) {
                List<GestoPagoProducto> productos = persistenceService.consultarCatalogo();
                guardarEnCache(productos);
                asegurarRespaldo(productos);
                log.info("Catálogo de GestoPago cargado de PostgreSQL a Redis");
            } else {
                asegurarRespaldo(enCache.get());
                log.info("Catálogo de GestoPago ya disponible en PostgreSQL y Redis");
            }
        } catch (GestoPagoIntegracionException e) {
            log.error("No se pudo cargar el catálogo de GestoPago al iniciar: {}", e.getMessage());
        }
    }

    /**
     * Copia el respaldo de Redis a PostgreSQL. Es la vía para poblar una base nueva sin depender
     * de getProductList (limitado a 3 llamadas diarias por PuntoRed).
     */
    public Optional<List<GestoPagoProducto>> restaurarDesdeRespaldo() {
        Optional<List<GestoPagoProducto>> respaldo = leerRespaldo().filter(productos -> !productos.isEmpty());
        if (respaldo.isEmpty()) {
            return Optional.empty();
        }
        persistenceService.guardarDesdeRespaldo(respaldo.get());
        List<GestoPagoProducto> productos = persistenceService.consultarCatalogo();
        guardarEnCache(productos);
        log.warn("Catálogo de GestoPago restaurado en PostgreSQL desde el respaldo de Redis: {} productos",
                productos.size());
        return Optional.of(productos);
    }

    @Scheduled(cron = "${gestopago.productos.sync-cron:-}")
    public void sincronizarProgramado() {
        try {
            sincronizarProductos();
        } catch (GestoPagoIntegracionException e) {
            log.error("Falló la sincronización programada del catálogo de GestoPago: {}", e.getMessage());
        }
    }

    private List<GestoPagoProducto> descargarSiFalta() {
        candadoDescarga.lock();
        try {
            List<GestoPagoProducto> productos = persistenceService.consultarCatalogo();
            if (!productos.isEmpty()) {
                guardarEnCache(productos);
                return productos;
            }
            Optional<List<GestoPagoProducto>> restaurados = restaurarDesdeRespaldo();
            if (restaurados.isPresent()) {
                return restaurados.get();
            }
            log.info("No hay productos de GestoPago en PostgreSQL ni respaldo en Redis; se descargará el catálogo");
            return sincronizarProductos();
        } finally {
            candadoDescarga.unlock();
        }
    }

    private List<GestoPagoProducto> descargarYGuardar() {
        candadoDescarga.lock();
        try {
            reservarIntentoDescarga();
            persistenceService.guardarCatalogo(xmlParser.parsear(descargarCatalogoXml()));
            List<GestoPagoProducto> productos = persistenceService.consultarCatalogo();
            guardarEnCache(productos);
            guardarRespaldo(productos);
            return productos;
        } finally {
            candadoDescarga.unlock();
        }
    }

    /**
     * PuntoRed bloquea la IP si getProductList se llama más de 3 veces al día. Cada intento, exitoso o no,
     * queda registrado en Redis (sobrevive a reinicios) y en memoria por si Redis no está disponible.
     */
    private void reservarIntentoDescarga() {
        if (intervaloDescargaMinutos <= 0) {
            return;
        }
        Duration intervalo = Duration.ofMinutes(intervaloDescargaMinutos);
        Instant ahora = Instant.now();
        if (ultimoIntentoDescarga != null && ahora.isBefore(ultimoIntentoDescarga.plus(intervalo))) {
            throw descargaPospuesta(ultimoIntentoDescarga.plus(intervalo));
        }
        Boolean reservado = null;
        try {
            reservado = redisTemplate.opsForValue().setIfAbsent(intentoKey, ahora.toString(), intervalo);
        } catch (RuntimeException e) {
            log.warn("No fue posible registrar el intento de descarga en Redis ({})", e.getClass().getSimpleName());
        }
        if (Boolean.FALSE.equals(reservado)) {
            throw descargaPospuesta(null);
        }
        ultimoIntentoDescarga = ahora;
    }

    private GestoPagoRespuestaException descargaPospuesta(Instant siguienteIntento) {
        String cuando = siguienteIntento == null ? "más tarde" : "después de " + siguienteIntento;
        log.warn("Se omite la descarga del catálogo de GestoPago: ya hubo un intento reciente; se podrá reintentar {}",
                cuando);
        return new GestoPagoRespuestaException(OPERACION_CATALOGO,
                "el catálogo no está disponible y ya se intentó descargar recientemente; se reintentará " + cuando);
    }

    private String descargarCatalogoXml() {
        String token = tokenService.obtenerTokenVigente().getToken();
        try {
            return productsIntegration.consultarCatalogoXml(token);
        } catch (GestoPagoAutenticacionException e) {
            log.warn("GestoPago rechazó el token guardado; se reintenta con uno nuevo");
            return productsIntegration.consultarCatalogoXml(tokenService.solicitarNuevoToken().getToken());
        }
    }

    private Optional<List<GestoPagoProducto>> leerCache() {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, tipoCatalogo));
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("No fue posible leer el catálogo de Redis; se usará PostgreSQL ({})", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private void guardarEnCache(List<GestoPagoProducto> productos) {
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(productos), cacheTtl);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("No fue posible guardar el catálogo en Redis ({})", e.getClass().getSimpleName());
        }
    }

    private Optional<List<GestoPagoProducto>> leerRespaldo() {
        try {
            String json = redisTemplate.opsForValue().get(respaldoKey);
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, tipoCatalogo));
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("No fue posible leer el respaldo del catálogo en Redis ({})", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /** Sin caducidad: debe sobrevivir aunque la base de datos se pierda. Nunca se reemplaza con una lista vacía. */
    private void guardarRespaldo(List<GestoPagoProducto> productos) {
        if (productos.isEmpty()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(respaldoKey, objectMapper.writeValueAsString(productos));
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("No fue posible guardar el respaldo del catálogo en Redis ({})", e.getClass().getSimpleName());
        }
    }

    private void asegurarRespaldo(List<GestoPagoProducto> productos) {
        if (leerRespaldo().isEmpty()) {
            guardarRespaldo(productos);
            log.info("Respaldo del catálogo de GestoPago creado en Redis: {} productos", productos.size());
        }
    }
}
