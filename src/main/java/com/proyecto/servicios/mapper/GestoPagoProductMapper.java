package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.GestoPagoProduct;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoXml;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface GestoPagoProductMapper {

    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "nuevo", ignore = true)
    @Mapping(target = "hasDigitoVerificador", defaultValue = "false")
    @Mapping(target = "showAyuda", defaultValue = "false")
    void updateEntity(GestoPagoProductoXml origen, @MappingTarget GestoPagoProduct entity);

    GestoPagoProducto toDto(GestoPagoProduct entity);
}
