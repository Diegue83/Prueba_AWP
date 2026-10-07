package com.proyecto.servicios.entity.onboarding.converter;

import com.proyecto.servicios.enums.Catalogo;
import com.proyecto.servicios.enums.EstatusCuenta;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Guarda el enum EstatusCuenta como su código de un carácter y lo reconstruye al leer.
 */
@Converter
public class EstatusCuentaConverter implements AttributeConverter<EstatusCuenta, String> {

    @Override
    public String convertToDatabaseColumn(EstatusCuenta valor) {
        return valor == null ? null : valor.getCodigo();
    }

    @Override
    public EstatusCuenta convertToEntityAttribute(String codigo) {
        return codigo == null ? null : Catalogo.leer(EstatusCuenta.class, codigo);
    }
}
