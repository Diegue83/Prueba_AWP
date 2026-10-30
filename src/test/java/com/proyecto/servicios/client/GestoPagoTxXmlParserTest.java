package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoTransaccionesProperties;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado.Tipo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GestoPagoTxXmlParserTest {

    private final GestoPagoTxXmlParser parser = new GestoPagoTxXmlParser(new GestoPagoTransaccionesProperties());

    @Test
    void interpretaUnaTransaccionAprobada() {
        GestoPagoTxResultado resultado = parser.interpretar("<?xml version='1.0' encoding='UTF-8'?><RESPONSE>"
                + "<ID_TX>778899</ID_TX><NUM_AUTORIZACION>123456</NUM_AUTORIZACION><SALDO>900.00</SALDO>"
                + "<MENSAJE><CODIGO>01</CODIGO><TEXTO>Transaccion exitosa</TEXTO><REFERENCIA/></MENSAJE></RESPONSE>");

        assertThat(resultado.tipo()).isEqualTo(Tipo.EXITOSO);
        assertThat(resultado.numAutorizacion()).isEqualTo("123456");
        assertThat(resultado.idTx()).isEqualTo("778899");
    }

    @Test
    void interpretaRechazosConLaFormaRealDelProveedor() {
        GestoPagoTxResultado resultado = parser.interpretar("<?xml version='1.0' encoding='UTF-8'?><RESPONSE> "
                + "<NUM_AUTORIZACION>-1</NUM_AUTORIZACION><MENSAJE><CODIGO>04</CODIGO>"
                + "<TEXTO>Parametros invalidos</TEXTO></MENSAJE></RESPONSE>");

        assertThat(resultado.tipo()).isEqualTo(Tipo.RECHAZADO);
        assertThat(resultado.codigo()).isEqualTo("04");
        assertThat(resultado.mensaje()).isEqualTo("Parametros invalidos");
    }

    @Test
    void laRespuestaCarrierNoRespondeEsIndeterminada() {
        GestoPagoTxResultado resultado = parser.interpretar("<?xml version='1.0' encoding='UTF-8'?><AUTORIZACION> "
                + "<NUM_AUTORIZACION>-1</NUM_AUTORIZACION><CODIGO_MENSAJE>03</CODIGO_MENSAJE>"
                + "<MENSAJE>Por el momento el carrier no responde, favor de intentar mas tarde.</MENSAJE></AUTORIZACION>");

        assertThat(resultado.tipo()).isEqualTo(Tipo.INDETERMINADO);
        assertThat(resultado.codigo()).isEqualTo("03");
        assertThat(resultado.mensaje()).startsWith("Por el momento");
    }

    @Test
    void unaRespuestaIlegibleNuncaSeTomaComoRechazo() {
        assertThat(parser.interpretar("").tipo()).isEqualTo(Tipo.INDETERMINADO);
        assertThat(parser.interpretar("<html>502 Bad Gateway").tipo()).isEqualTo(Tipo.INDETERMINADO);
        assertThat(parser.interpretar("<RESPONSE><ID_TX>1</ID_TX></RESPONSE>").tipo()).isEqualTo(Tipo.INDETERMINADO);
        assertThat(parser.interpretar("<?xml version='1.0'?><!DOCTYPE r [<!ENTITY x SYSTEM 'file:///etc/passwd'>]>"
                + "<RESPONSE><MENSAJE><CODIGO>&x;</CODIGO></MENSAJE></RESPONSE>").tipo()).isEqualTo(Tipo.INDETERMINADO);
    }
}
