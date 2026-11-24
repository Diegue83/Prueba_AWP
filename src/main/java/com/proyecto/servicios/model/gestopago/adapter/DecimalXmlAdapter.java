package com.proyecto.servicios.model.gestopago.adapter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.math.BigDecimal;

public class DecimalXmlAdapter extends XmlAdapter<String, BigDecimal> {

    @Override
    public BigDecimal unmarshal(String valor) {
        try {
            return valor == null ? null : new BigDecimal(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String marshal(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
