package com.proyecto.servicios.security;

import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.NoAutenticadoException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ControlAccesoTest {

    private final ControlAcceso controlAcceso = new ControlAcceso();
    private HttpServletRequest request;

    @BeforeEach
    void prepararPeticion() {
        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void limpiarPeticion() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void rechazaAccesoSinClienteAutenticado() {
        assertThatThrownBy(() -> controlAcceso.verificarPropietario(1))
                .isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    void rechazaElRecursoDeOtroCliente() {
        request.setAttribute(AutenticacionInterceptor.CLIENTE_AUTENTICADO, 1);

        assertThatThrownBy(() -> controlAcceso.verificarPropietario(2))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void permiteElRecursoDelClienteAutenticado() {
        request.setAttribute(AutenticacionInterceptor.CLIENTE_AUTENTICADO, 1);

        controlAcceso.verificarPropietario(1);
    }
}
