package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoTransaccionesProperties;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado.Tipo;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

/**
 * Interpreta la respuesta de sendTx/confirmTx. El proveedor usa dos formas:
 * <pre>
 * &lt;RESPONSE&gt;&lt;NUM_AUTORIZACION/&gt;&lt;ID_TX/&gt;&lt;MENSAJE&gt;&lt;CODIGO/&gt;&lt;TEXTO/&gt;&lt;/MENSAJE&gt;&lt;/RESPONSE&gt;
 * &lt;AUTORIZACION&gt;&lt;NUM_AUTORIZACION/&gt;&lt;CODIGO_MENSAJE/&gt;&lt;MENSAJE/&gt;&lt;/AUTORIZACION&gt;
 * </pre>
 * Una respuesta que no se entiende es INDETERMINADA, no un rechazo.
 */
@Component
public class GestoPagoTxXmlParser {

    private static final String CODIGO_EXITO = "01";

    private final GestoPagoTransaccionesProperties properties;
    private final DocumentBuilderFactory factory;

    public GestoPagoTxXmlParser(GestoPagoTransaccionesProperties properties) {
        this.properties = properties;
        this.factory = crearFactory();
    }

    public GestoPagoTxResultado interpretar(String xml) {
        if (xml == null || xml.isBlank()) {
            return GestoPagoTxResultado.indeterminado("respuesta vacía");
        }
        Element raiz;
        try {
            Document documento = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            raiz = documento.getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return GestoPagoTxResultado.indeterminado("respuesta con formato desconocido");
        }

        Element mensaje = hijo(raiz, "MENSAJE");
        String codigo = mensaje != null && hijo(mensaje, "CODIGO") != null
                ? texto(hijo(mensaje, "CODIGO"))
                : texto(hijo(raiz, "CODIGO_MENSAJE"));
        String descripcion = mensaje != null && hijo(mensaje, "TEXTO") != null
                ? texto(hijo(mensaje, "TEXTO"))
                : texto(mensaje);
        String numAutorizacion = texto(hijo(raiz, "NUM_AUTORIZACION"));
        String idTx = texto(hijo(raiz, "ID_TX"));

        return new GestoPagoTxResultado(clasificar(codigo), codigo, descripcion, numAutorizacion, idTx);
    }

    private Tipo clasificar(String codigo) {
        if (codigo == null) {
            return Tipo.INDETERMINADO;
        }
        if (CODIGO_EXITO.equals(codigo)) {
            return Tipo.EXITOSO;
        }
        return properties.getCodigosIndeterminados().contains(codigo) ? Tipo.INDETERMINADO : Tipo.RECHAZADO;
    }

    private Element hijo(Element padre, String nombre) {
        if (padre == null) {
            return null;
        }
        NodeList hijos = padre.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node nodo = hijos.item(i);
            if (nodo.getNodeType() == Node.ELEMENT_NODE && nombre.equals(nodo.getNodeName())) {
                return (Element) nodo;
            }
        }
        return null;
    }

    private String texto(Element elemento) {
        if (elemento == null) {
            return null;
        }
        String valor = elemento.getTextContent().trim();
        return valor.isEmpty() ? null : valor;
    }

    private static DocumentBuilderFactory crearFactory() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("No fue posible configurar el lector XML", e);
        }
    }
}
