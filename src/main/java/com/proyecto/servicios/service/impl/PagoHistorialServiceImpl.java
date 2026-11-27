package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.entity.pagos.Pago;
import com.proyecto.servicios.exception.PagoNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.pagos.PagoResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoProductRepository;
import com.proyecto.servicios.repositorys.pagos.PagoRepository;
import com.proyecto.servicios.service.PagoHistorialService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PagoHistorialServiceImpl implements PagoHistorialService {

    public static final int TAMANO_MAXIMO = 50;

    private final PagoRepository pagoRepository;
    private final GestoPagoProductRepository productRepository;

    public PagoHistorialServiceImpl(PagoRepository pagoRepository, GestoPagoProductRepository productRepository) {
        this.pagoRepository = pagoRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> consultarHistorial(Integer clienteId, int pagina, int tamano) {
        List<String> errores = new ArrayList<>();
        if (pagina < 0) {
            errores.add("pagina: debe ser 0 o mayor");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            errores.add("tamano: debe estar entre 1 y " + TAMANO_MAXIMO);
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException("Los parámetros de paginación no son válidos", errores);
        }

        List<Pago> pagos = pagoRepository.findByClienteIdOrderByFechaCreacionDescIdDesc(
                clienteId, PageRequest.of(pagina, tamano));
        Map<Integer, GestoPagoProduct> productos = productRepository
                .findAllById(pagos.stream().map(Pago::getIdProducto).distinct().toList())
                .stream()
                .collect(Collectors.toMap(GestoPagoProduct::getIdProducto, Function.identity()));
        return pagos.stream()
                .map(pago -> PagoResponse.de(pago, productos.get(pago.getIdProducto())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse consultarDetalle(Integer clienteId, Long idPago) {
        Pago pago = pagoRepository.findByIdAndClienteId(idPago, clienteId)
                .orElseThrow(() -> new PagoNoEncontradoException(idPago));
        return PagoResponse.de(pago, productRepository.findById(pago.getIdProducto()).orElse(null));
    }
}
