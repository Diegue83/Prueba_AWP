package com.proyecto.servicios.entity.onboarding.converter;

import com.proyecto.servicios.enums.Catalogo;
import com.proyecto.servicios.enums.Sexo;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Guarda el enum Sexo como su código de un carácter y lo reconstruye al leer.
 */
@Converter
public class SexoConverter implements AttributeConverter<Sexo, String> {

    @Override
    public String convertToDatabaseColumn(Sexo valor) {
        return valor == null ? null : valor.getCodigo();
    }

    @Override
    public Sexo convertToEntityAttribute(String codigo) {
        return codigo == null ? null : Catalogo.leer(Sexo.class, codigo);
    }
}
