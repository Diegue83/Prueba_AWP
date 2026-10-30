package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.exception.ProductoNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.pagos.PagoRequest;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas que dependen del catálogo. El producto se lee de PostgreSQL y no de Redis:
 * la cache es para consultas, pero una regla de dinero no debe depender de un dato que
 * alguien con acceso a Redis podría alterar.
 */
@Component
public class PagoValidador {

    private static final String REFERENCIA_TELEFONO = "a";

    private final GestoPagoProductRepository productRepository;
    private final PagosProperties properties;

    public PagoValidador(GestoPagoProductRepository productRepository, PagosProperties properties) {
        this.productRepository = productRepository;
        this.properties = properties;
    }

    public GestoPagoProduct validar(PagoRequest request) {
        GestoPagoProduct producto = productRepository.findById(request.getIdProducto())
                .orElseThrow(() -> new ProductoNoEncontradoException(request.getIdProducto()));

        List<String> errores = new ArrayList<>();
        if (!producto.getIdServicio().equals(request.getIdServicio())) {
            errores.add("idServicio: no corresponde al producto " + producto.getIdProducto());
        }
        if (!properties.getTiposFrontPermitidos().contains(producto.getTipoFront())) {
            errores.add("idProducto: este tipo de producto no se puede pagar por este medio");
        }
        validarMonto(producto, request, errores);
        validarDestino(producto, request, errores);

        if (!errores.isEmpty()) {
            throw new ValidacionException("La operación no cumple las reglas del producto", errores);
        }
        return producto;
    }

    /** Precio fijo: debe coincidir exacto. Precio 0 en el catálogo: monto abierto dentro de los límites. */
    private void validarMonto(GestoPagoProduct producto, PagoRequest request, List<String> errores) {
        if (producto.getPrecio().signum() > 0) {
            if (producto.getPrecio().compareTo(request.getMonto()) != 0) {
                errores.add("monto: el producto tiene un precio fijo de " + producto.getPrecio().toPlainString());
            }
            return;
        }
        if (request.getMonto().compareTo(properties.getMontoMinimo()) < 0
                || request.getMonto().compareTo(properties.getMontoMaximo()) > 0) {
            errores.add("monto: debe estar entre " + properties.getMontoMinimo().toPlainString()
                    + " y " + properties.getMontoMaximo().toPlainString());
        }
    }

    /** tipoReferencia "a" identifica al destino por número telefónico; los demás por referencia. */
    private void validarDestino(GestoPagoProduct producto, PagoRequest request, List<String> errores) {
        if (REFERENCIA_TELEFONO.equals(producto.getTipoReferencia())) {
            if (request.getTelefono() == null) {
                errores.add("telefono: es obligatorio para este producto");
            }
        } else if (request.getReferencia() == null) {
            errores.add("referencia: es obligatoria para este producto");
        }
    }
}
