package com.proyecto.servicios.security;

import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.NoAutenticadoException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** Verifica que el recurso solicitado pertenezca al cliente que inició sesión. */
@Component
public class ControlAcceso {

    public void verificarPropietario(Integer clienteIdDelRecurso) {
        if (!clienteAutenticado().equals(clienteIdDelRecurso)) {
            throw new AccesoDenegadoException();
        }
    }

    private Integer clienteAutenticado() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        Object valor = atributos == null ? null
                : atributos.getAttribute(AutenticacionInterceptor.CLIENTE_AUTENTICADO, RequestAttributes.SCOPE_REQUEST);
        if (!(valor instanceof Integer clienteId)) {
            throw new NoAutenticadoException("Inicia sesión para acceder a este recurso");
        }
        return clienteId;
    }

    public Integer obtenerClienteAutenticado() {
        return clienteAutenticado();
    }
}