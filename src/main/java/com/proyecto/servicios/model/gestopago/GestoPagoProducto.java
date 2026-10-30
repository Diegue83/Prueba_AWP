package com.proyecto.servicios.model.gestopago;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GestoPagoProducto {

    private Integer idProducto;
    private String producto;
    private Integer idServicio;
    private String servicio;
    private Integer idCatTipoServicio;
    private BigDecimal precio;
    private Integer tipoFront;
    private String tipoReferencia;
    private Boolean hasDigitoVerificador;
    private Boolean showAyuda;
    private String leyenda;
}
