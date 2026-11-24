package com.proyecto.servicios.model.gestopago;

import com.proyecto.servicios.model.gestopago.adapter.DecimalXmlAdapter;
import com.proyecto.servicios.model.gestopago.adapter.EnteroXmlAdapter;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Data;

import java.math.BigDecimal;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoProductoXml {

    @XmlAttribute
    @XmlJavaTypeAdapter(EnteroXmlAdapter.class)
    private Integer idProducto;

    @XmlAttribute
    private String producto;

    @XmlAttribute
    @XmlJavaTypeAdapter(EnteroXmlAdapter.class)
    private Integer idServicio;

    @XmlAttribute
    private String servicio;

    @XmlAttribute
    @XmlJavaTypeAdapter(EnteroXmlAdapter.class)
    private Integer idCatTipoServicio;

    @XmlAttribute
    @XmlJavaTypeAdapter(DecimalXmlAdapter.class)
    private BigDecimal precio;

    @XmlAttribute
    @XmlJavaTypeAdapter(EnteroXmlAdapter.class)
    private Integer tipoFront;

    @XmlAttribute
    private String tipoReferencia;

    @XmlAttribute
    private Boolean hasDigitoVerificador;

    @XmlAttribute
    private Boolean showAyuda;

    @XmlElement(name = "legend")
    private String leyenda;

    public boolean tieneDatosObligatorios() {
        return idProducto != null && idServicio != null && idCatTipoServicio != null
                && tipoFront != null && precio != null && precio.signum() >= 0
                && servicio != null && !servicio.isBlank()
                && producto != null && !producto.isBlank();
    }
}
