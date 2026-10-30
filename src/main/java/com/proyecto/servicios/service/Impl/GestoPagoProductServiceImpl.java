package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoProductsIntegration;
import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.exception.GestoPagoIntegracionException;
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
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoProductServiceImpl implements GestoPagoProductService {

    private static final String CACHE_KEY_PREFIX = "gestopago:productos:";

    private final GestoPagoProductsIntegration productsIntegration;
    private final GestoPagoTokenService tokenService;
    private final GestoPagoProductPersistenceService persistenceService;
    private final GestoPagoProductXmlParser xmlParser;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final JavaType tipoCatalogo;
    private final String cacheKey;
    private final Duration cacheTtl;

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
            log.info("No hay productos de GestoPago en PostgreSQL; se descargará el catálogo");
            return sincronizarProductos();
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
     * solo se carga a Redis cuando ahí no existe.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void cargarCatalogoAlIniciar() {
        try {
            if (!persistenceService.existeCatalogo()) {
                sincronizarProductos();
            } else if (leerCache().isEmpty()) {
                guardarEnCache(persistenceService.consultarCatalogo());
                log.info("Catálogo de GestoPago cargado de PostgreSQL a Redis");
            } else {
                log.info("Catálogo de GestoPago ya disponible en PostgreSQL y Redis");
            }
        } catch (GestoPagoIntegracionException e) {
            log.error("No se pudo cargar el catálogo de GestoPago al iniciar: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "${gestopago.productos.sync-cron:-}")
    public void sincronizarProgramado() {
        try {
            sincronizarProductos();
        } catch (GestoPagoIntegracionException e) {
            log.error("Falló la sincronización programada del catálogo de GestoPago: {}", e.getMessage());
        }
    }

    private List<GestoPagoProducto> descargarYGuardar() {
        persistenceService.guardarCatalogo(xmlParser.parsear(descargarCatalogoXml()));
        List<GestoPagoProducto> productos = persistenceService.consultarCatalogo();
        guardarEnCache(productos);
        return productos;
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
        } catch (Exception e) {
            log.warn("No fue posible leer el catálogo de Redis; se usará PostgreSQL ({})", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private void guardarEnCache(List<GestoPagoProducto> productos) {
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(productos), cacheTtl);
        } catch (Exception e) {
            log.warn("No fue posible guardar el catálogo en Redis ({})", e.getClass().getSimpleName());
        }
    }
}
