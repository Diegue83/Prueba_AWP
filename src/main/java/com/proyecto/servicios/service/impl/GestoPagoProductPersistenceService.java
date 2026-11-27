package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.mapper.GestoPagoProductMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoXml;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GestoPagoProductPersistenceService {

    private final GestoPagoProductRepository productRepository;
    private final GestoPagoProductMapper productMapper;

    public GestoPagoProductPersistenceService(GestoPagoProductRepository productRepository,
                                              GestoPagoProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    /**
     * Inserta los productos nuevos y actualiza los existentes por id_producto. Hibernate solo
     * emite UPDATE en las filas que cambiaron, así que una resincronización no reescribe datos iguales.
     */
    @Transactional
    public void guardarCatalogo(List<GestoPagoProductoXml> productos) {
        guardar(productos, GestoPagoProductoXml::getIdProducto, productMapper::updateEntity);
    }

    /** Carga en PostgreSQL el respaldo del catálogo guardado en Redis (misma lógica de inserción/actualización). */
    @Transactional
    public void guardarDesdeRespaldo(List<GestoPagoProducto> productos) {
        guardar(productos, GestoPagoProducto::getIdProducto, productMapper::updateEntity);
    }

    private <T> void guardar(List<T> productos, Function<T, Integer> idProducto,
                             BiConsumer<T, GestoPagoProduct> copiar) {
        Map<Integer, GestoPagoProduct> existentes = productRepository
                .findAllById(productos.stream().map(idProducto).toList())
                .stream()
                .collect(Collectors.toMap(GestoPagoProduct::getIdProducto, Function.identity()));

        List<GestoPagoProduct> entidades = productos.stream()
                .map(producto -> {
                    GestoPagoProduct entidad = existentes.getOrDefault(idProducto.apply(producto), new GestoPagoProduct());
                    copiar.accept(producto, entidad);
                    return entidad;
                })
                .toList();
        productRepository.saveAll(entidades);
        log.info("Catálogo GestoPago en PostgreSQL: {} recibidos, {} nuevos",
                productos.size(), productos.size() - existentes.size());
    }

    @Transactional(readOnly = true)
    public List<GestoPagoProducto> consultarCatalogo() {
        return productRepository.findAllByOrderByServicioAscPrecioAscIdProductoAsc().stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean existeCatalogo() {
        return productRepository.count() > 0;
    }
}
