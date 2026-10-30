package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoProducto;

import java.util.List;

public interface GestoPagoProductService {

    List<GestoPagoProducto> consultarProductos();

    List<GestoPagoProducto> sincronizarProductos();
}
