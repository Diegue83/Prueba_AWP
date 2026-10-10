package com.proyecto.servicios.security;

import com.proyecto.servicios.exception.NoAutenticadoException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AutenticacionInterceptorTest {

    private final TokenService tokenService = mock(TokenService.class);
    private final AutenticacionInterceptor interceptor = new AutenticacionInterceptor(tokenService);
    private final HttpServletResponse response = new MockHttpServletResponse();

    @Test
    void protegeElListadoDeClientesSinToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/clientes");
        request.setServletPath("/clientes");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(NoAutenticadoException.class);

        verifyNoInteractions(tokenService);
    }

    @Test
    void permiteElAltaPublicaDeClientesSinToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/clientes");
        request.setServletPath("/clientes");

        interceptor.preHandle(request, response, new Object());

        verifyNoInteractions(tokenService);
    }

    @Test
    void almacenaElClienteIdentificadoPorElTokenEnLaPeticion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/clientes/12");
        request.setServletPath("/clientes/12");
        request.addHeader("Authorization", "Bearer token-valido");
        when(tokenService.obtenerClienteId("token-valido")).thenReturn(12);

        interceptor.preHandle(request, response, new Object());

        verify(tokenService).obtenerClienteId("token-valido");
        org.assertj.core.api.Assertions.assertThat(
                request.getAttribute(AutenticacionInterceptor.CLIENTE_AUTENTICADO)).isEqualTo(12);
    }
}
