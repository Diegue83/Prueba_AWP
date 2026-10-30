package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductsIntegration;
import com.proyecto.servicios.exception.GestoPagoRespuestaException;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoXml;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GestoPagoProductXmlParser {

    private static final String OPERACION = GestoPagoProductsIntegration.OPERACION;
    private static final String RAIZ = "RESPONSE";

    private final JAXBContext context;
    private final XMLInputFactory inputFactory;

    public GestoPagoProductXmlParser() {
        try {
            context = JAXBContext.newInstance(GestoPagoProductListResponse.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("No fue posible inicializar el lector XML de productos", e);
        }
        inputFactory = XMLInputFactory.newFactory();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    }

    public List<GestoPagoProductoXml> parsear(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new GestoPagoRespuestaException(OPERACION, "respuesta vacía");
        }

        GestoPagoProductListResponse respuesta = leer(xml);
        GestoPagoProductListResponse.Mensaje mensaje = respuesta.getMensaje();
        if (mensaje == null || !GestoPagoProductListResponse.CODIGO_EXITO.equals(trim(mensaje.getCodigo()))) {
            String motivo = mensaje == null
                    ? "la respuesta no trae MENSAJE"
                    : "código " + trim(mensaje.getCodigo()) + " - " + trim(mensaje.getTexto());
            throw new GestoPagoRespuestaException(OPERACION, motivo);
        }

        List<GestoPagoProductoXml> validos = respuesta.getProductos().stream()
                .filter(GestoPagoProductoXml::tieneDatosObligatorios)
                .map(this::limpiarLeyenda)
                .collect(Collectors.toMap(GestoPagoProductoXml::getIdProducto, Function.identity(),
                        (anterior, siguiente) -> siguiente, LinkedHashMap::new))
                .values().stream()
                .toList();
        int descartados = respuesta.getProductos().size() - validos.size();
        if (descartados > 0) {
            log.warn("Se descartaron {} productos de GestoPago sin datos obligatorios o repetidos", descartados);
        }
        return validos;
    }

    private GestoPagoProductoXml limpiarLeyenda(GestoPagoProductoXml producto) {
        producto.setLeyenda(trim(producto.getLeyenda()));
        return producto;
    }

    private GestoPagoProductListResponse leer(String xml) {
        XMLStreamReader reader = null;
        try {
            reader = inputFactory.createXMLStreamReader(new StringReader(xml));
            reader.nextTag();
            if (!RAIZ.equals(reader.getLocalName())) {
                throw new GestoPagoRespuestaException(OPERACION, "el XML recibido no tiene el formato esperado");
            }
            return context.createUnmarshaller()
                    .unmarshal(reader, GestoPagoProductListResponse.class)
                    .getValue();
        } catch (XMLStreamException | JAXBException e) {
            throw new GestoPagoRespuestaException(OPERACION, "el XML recibido no tiene el formato esperado");
        } finally {
            cerrar(reader);
        }
    }

    private void cerrar(XMLStreamReader reader) {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (XMLStreamException e) {
            log.debug("No se pudo cerrar el lector XML", e);
        }
    }

    private String trim(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
