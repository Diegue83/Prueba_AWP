package com.proyecto.servicios.model.gestopago.adapter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class EnteroXmlAdapter extends XmlAdapter<String, Integer> {

    @Override
    public Integer unmarshal(String valor) {
        try {
            return valor == null ? null : Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String marshal(Integer valor) {
        return valor == null ? null : valor.toString();
    }
}
