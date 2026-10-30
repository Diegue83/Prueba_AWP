package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.exception.ProductoNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.pagos.PagoRequest;
import com.proyecto.servicios.model.pagos.PagoValidado;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reglas de la especificación de PuntoRed para sendTx. El producto se lee de PostgreSQL y no de
 * Redis: una regla de dinero no debe depender de un dato que se podría alterar en la cache.
 * <ul>
 *   <li>tipoFront 1 (recargas): el precio del catálogo es el monto a cobrar y el teléfono o la
 *       referencia viajan en el campo "telefono".</li>
 *   <li>tipoFront 2 (pagos de servicio): el monto lo define el recibo, el precio del catálogo es la
 *       comisión, "telefono" va fijo en 1111111111 y la referencia en "referencia".</li>
 *   <li>idCatTipoServicio 10 y 11 (artículos canjeables): "telefono" va fijo en #1111111111.</li>
 * </ul>
 */
@Component
public class PagoValidador {

    public static final int FRONT_RECARGA = 1;
    public static final int FRONT_PAGO_SERVICIO = 2;
    private static final String TELEFONO_FIJO = "1111111111";
    private static final String TELEFONO_CANJEABLE = "#1111111111";
    private static final Set<Integer> TIPOS_CANJEABLES = Set.of(10, 11);
    private static final Set<String> REFERENCIA_EN_TELEFONO = Set.of("a", "ab");

    private final GestoPagoProductRepository productRepository;
    private final PagosProperties properties;

    public PagoValidador(GestoPagoProductRepository productRepository, PagosProperties properties) {
        this.productRepository = productRepository;
        this.properties = properties;
    }

    public PagoValidado validar(PagoRequest request) {
        GestoPagoProduct producto = productRepository.findById(request.getIdProducto())
                .orElseThrow(() -> new ProductoNoEncontradoException(request.getIdProducto()));

        List<String> errores = new ArrayList<>();
        if (!producto.getIdServicio().equals(request.getIdServicio())) {
            errores.add("idServicio: no corresponde al producto " + producto.getIdProducto());
        }
        if (!properties.getTiposFrontPermitidos().contains(producto.getTipoFront())) {
            errores.add("idProducto: este tipo de producto no se puede pagar por este medio");
        } else {
            validarMonto(producto, request, errores);
            validarDestino(producto, request, errores);
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException("La operación no cumple las reglas del producto", errores);
        }

        boolean esPagoServicio = producto.getTipoFront() == FRONT_PAGO_SERVICIO;
        return new PagoValidado(
                producto.getIdProducto(),
                producto.getIdServicio(),
                producto.getTipoFront(),
                request.getMonto(),
                esPagoServicio ? producto.getPrecio() : BigDecimal.ZERO,
                telefonoParaEnvio(producto, request),
                esPagoServicio ? request.getReferencia() : null);
    }

    private void validarMonto(GestoPagoProduct producto, PagoRequest request, List<String> errores) {
        if (producto.getTipoFront() == FRONT_RECARGA) {
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

    private void validarDestino(GestoPagoProduct producto, PagoRequest request, List<String> errores) {
        if (esCanjeable(producto)) {
            return;
        }
        if (producto.getTipoFront() == FRONT_PAGO_SERVICIO || !referenciaEnTelefono(producto)) {
            if (request.getReferencia() == null) {
                errores.add("referencia: es obligatoria para este producto");
            }
        } else if (request.getTelefono() == null) {
            errores.add("telefono: es obligatorio para este producto");
        }
    }

    private String telefonoParaEnvio(GestoPagoProduct producto, PagoRequest request) {
        if (esCanjeable(producto)) {
            return TELEFONO_CANJEABLE;
        }
        if (producto.getTipoFront() == FRONT_PAGO_SERVICIO) {
            return TELEFONO_FIJO;
        }
        return referenciaEnTelefono(producto) ? request.getTelefono() : request.getReferencia();
    }

    private boolean esCanjeable(GestoPagoProduct producto) {
        return TIPOS_CANJEABLES.contains(producto.getIdCatTipoServicio());
    }

    private boolean referenciaEnTelefono(GestoPagoProduct producto) {
        return REFERENCIA_EN_TELEFONO.contains(producto.getTipoReferencia());
    }
}
