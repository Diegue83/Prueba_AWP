package com.proyecto.servicios.service;

import com.proyecto.servicios.exception.GestoPagoRespuestaException;
import com.proyecto.servicios.model.gestopago.GestoPagoProductoXml;
import com.proyecto.servicios.service.Impl.GestoPagoProductXmlParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestoPagoProductXmlParserTest {

    static final String XML_CATALOGO = "<?xml version='1.0' encoding='UTF-8'?><RESPONSE> <MENSAJE><CODIGO>01</CODIGO>"
            + "<TEXTO>Operacion realizada con exito</TEXTO></MENSAJE><PRODUCTOS>"
            + "<producto servicio='ABIB' producto='ABIB 100' idServicio='2284' idProducto='14302' idCatTipoServicio='13'"
            + " tipoFront='1' hasDigitoVerificador='false' precio='100.0' showAyuda='false' tipoReferencia='a'>"
            + "<legend><![CDATA[Recibe soporte las 24h del dia. Conserva tu comprobante de pago.]]></legend></producto>"
            + "<producto servicio='AT-T Pospago' producto='AT-T Pospago' idServicio='105' idProducto='10034'"
            + " idCatTipoServicio='2' tipoFront='4' hasDigitoVerificador='false' precio='0.0' showAyuda='true'"
            + " tipoReferencia='bc'><legend><![CDATA[ ]]></legend></producto>"
            + "</PRODUCTOS></RESPONSE>";

    private final GestoPagoProductXmlParser parser = new GestoPagoProductXmlParser();

    @Test
    void convierteLosAtributosDeCadaProductoATiposDeDatos() {
        List<GestoPagoProductoXml> productos = parser.parsear(XML_CATALOGO);

        assertThat(productos).hasSize(2);
        GestoPagoProductoXml abib = productos.get(0);
        assertThat(abib.getIdProducto()).isEqualTo(14302);
        assertThat(abib.getIdServicio()).isEqualTo(2284);
        assertThat(abib.getIdCatTipoServicio()).isEqualTo(13);
        assertThat(abib.getServicio()).isEqualTo("ABIB");
        assertThat(abib.getProducto()).isEqualTo("ABIB 100");
        assertThat(abib.getPrecio()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(abib.getTipoFront()).isEqualTo(1);
        assertThat(abib.getTipoReferencia()).isEqualTo("a");
        assertThat(abib.getHasDigitoVerificador()).isFalse();
        assertThat(abib.getShowAyuda()).isFalse();
        assertThat(abib.getLeyenda()).startsWith("Recibe soporte");
        assertThat(productos.get(1).getShowAyuda()).isTrue();
        assertThat(productos.get(1).getLeyenda()).isNull();
    }

    @Test
    void descartaProductosSinDatosObligatoriosYRepetidos() {
        String xml = "<RESPONSE><MENSAJE><CODIGO>01</CODIGO><TEXTO>ok</TEXTO></MENSAJE><PRODUCTOS>"
                + "<producto servicio='A' producto='A 10' idServicio='1' idProducto='1' idCatTipoServicio='1' tipoFront='1' precio='10'/>"
                + "<producto servicio='A' producto='A 10 bis' idServicio='1' idProducto='1' idCatTipoServicio='1' tipoFront='1' precio='10'/>"
                + "<producto servicio='B' producto='B' idServicio='2' idCatTipoServicio='1' tipoFront='1' precio='5'/>"
                + "<producto servicio='C' producto='C' idServicio='3' idProducto='3' idCatTipoServicio='1' tipoFront='1' precio='abc'/>"
                + "</PRODUCTOS></RESPONSE>";

        List<GestoPagoProductoXml> productos = parser.parsear(xml);

        assertThat(productos).extracting(GestoPagoProductoXml::getProducto).containsExactly("A 10 bis");
    }

    @Test
    void aceptaCatalogoVacio() {
        String xml = "<RESPONSE><MENSAJE><CODIGO>01</CODIGO><TEXTO>ok</TEXTO></MENSAJE><PRODUCTOS/></RESPONSE>";

        assertThat(parser.parsear(xml)).isEmpty();
    }

    @Test
    void rechazaCodigoDeRespuestaNoExitoso() {
        String xml = "<RESPONSE><MENSAJE><CODIGO>05</CODIGO><TEXTO>Distribuidor inactivo</TEXTO></MENSAJE></RESPONSE>";

        assertThatThrownBy(() -> parser.parsear(xml))
                .isInstanceOf(GestoPagoRespuestaException.class)
                .hasMessageContaining("código 05 - Distribuidor inactivo");
    }

    @Test
    void rechazaRespuestaSinMensaje() {
        assertThatThrownBy(() -> parser.parsear("<RESPONSE><PRODUCTOS/></RESPONSE>"))
                .isInstanceOf(GestoPagoRespuestaException.class)
                .hasMessageContaining("no trae MENSAJE");
    }

    @Test
    void rechazaRespuestaVaciaOConFormatoInvalido() {
        assertThatThrownBy(() -> parser.parsear("  "))
                .isInstanceOf(GestoPagoRespuestaException.class)
                .hasMessageContaining("respuesta vacía");
        assertThatThrownBy(() -> parser.parsear("{\"success\":false}"))
                .isInstanceOf(GestoPagoRespuestaException.class)
                .hasMessageContaining("formato esperado");
        assertThatThrownBy(() -> parser.parsear("<OTRO><MENSAJE><CODIGO>01</CODIGO></MENSAJE></OTRO>"))
                .isInstanceOf(GestoPagoRespuestaException.class);
    }

    @Test
    void noResuelveEntidadesExternas() {
        String xml = "<?xml version='1.0'?><!DOCTYPE r [<!ENTITY x SYSTEM 'file:///etc/passwd'>]>"
                + "<RESPONSE><MENSAJE><CODIGO>01</CODIGO><TEXTO>&x;</TEXTO></MENSAJE></RESPONSE>";

        assertThatThrownBy(() -> parser.parsear(xml)).isInstanceOf(GestoPagoRespuestaException.class);
    }
}
