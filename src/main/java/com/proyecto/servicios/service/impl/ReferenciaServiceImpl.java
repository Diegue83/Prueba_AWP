package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.client.GestoPagoCifrador;
import com.proyecto.servicios.client.GestoPagoTransaccionesIntegration;
import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.exception.GestoPagoAutenticacionException;
import com.proyecto.servicios.exception.ProductoNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.gestopago.GestoPagoReferenciaResultado;
import com.proyecto.servicios.model.gestopago.GestoPagoTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import com.proyecto.servicios.model.pagos.ReferenciaRequest;
import com.proyecto.servicios.model.pagos.ReferenciaResponse;
import com.proyecto.servicios.model.pagos.ReferenciaResponse.Estado;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.ReferenciaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class ReferenciaServiceImpl implements ReferenciaService {

    /** tipoFront de PuntoRed para los productos de consulta: son los únicos que acepta verifyReference. */
    public static final int FRONT_CONSULTA_REFERENCIA = 4;

    private static final String SIN_RESPUESTA =
            "No fue posible verificar la referencia con el proveedor; intenta de nuevo en unos minutos";

    private final GestoPagoProductRepository productRepository;
    private final GestoPagoTransaccionesIntegration integracion;
    private final GestoPagoCifrador cifrador;
    private final GestoPagoTokenService tokenService;

    public ReferenciaServiceImpl(GestoPagoProductRepository productRepository,
                                 GestoPagoTransaccionesIntegration integracion,
                                 GestoPagoCifrador cifrador,
                                 GestoPagoTokenService tokenService) {
        this.productRepository = productRepository;
        this.integracion = integracion;
        this.cifrador = cifrador;
        this.tokenService = tokenService;
    }

    @Override
    public ReferenciaResponse verificar(ReferenciaRequest request) {
        GestoPagoProduct producto = productRepository.findById(request.getIdProducto())
                .orElseThrow(() -> new ProductoNoEncontradoException(request.getIdProducto()));
        if (!producto.getIdServicio().equals(request.getIdServicio())) {
            throw new ValidacionException("El producto no pertenece al servicio indicado",
                    List.of("idServicio: no corresponde al producto " + request.getIdProducto()));
        }
        cifrador.validarConfiguracion();

        String referencia = request.getReferencia().trim().toUpperCase(Locale.ROOT);
        GestoPagoProduct consulta = productoDeConsulta(producto);
        GestoPagoTxPayload payload = new GestoPagoTxPayload(consulta.getIdServicio(), consulta.getIdProducto(),
                referencia, null, null, null, null, null);
        GestoPagoReferenciaResultado verificacion = conToken(payload);
        GestoPagoTxResultado resultado = verificacion.resultado();

        Estado estado;
        String mensaje;
        if (resultado.esExitoso()) {
            estado = Estado.VALIDA;
            mensaje = resultado.mensaje() == null ? "Referencia válida" : resultado.mensaje();
        } else if (resultado.esRechazado()) {
            estado = Estado.INVALIDA;
            mensaje = resultado.mensaje() == null ? "El proveedor rechazó la referencia" : resultado.mensaje();
        } else {
            estado = Estado.NO_VERIFICADA;
            mensaje = SIN_RESPUESTA;
        }
        log.info("Verificación de referencia idProducto={} resultado={} código={}",
                producto.getIdProducto(), estado, resultado.codigo());
        return new ReferenciaResponse(estado, estado == Estado.VALIDA, producto.getIdServicio(),
                producto.getIdProducto(), producto.getServicio(), producto.getProducto(), referencia,
                resultado.codigo(), mensaje, verificacion.saldo());
    }

    private GestoPagoProduct productoDeConsulta(GestoPagoProduct producto) {
        if (Integer.valueOf(FRONT_CONSULTA_REFERENCIA).equals(producto.getTipoFront())) {
            return producto;
        }
        return productRepository
                .findFirstByIdServicioAndTipoFrontOrderByIdProductoAsc(producto.getIdServicio(), FRONT_CONSULTA_REFERENCIA)
                .orElse(producto);
    }

    private GestoPagoReferenciaResultado conToken(GestoPagoTxPayload payload) {
        try {
            return integracion.verificarReferencia(payload, tokenService.obtenerTokenVigente().getToken());
        } catch (GestoPagoAutenticacionException e) {
            return integracion.verificarReferencia(payload, tokenService.solicitarNuevoToken().getToken());
        }
    }
}
