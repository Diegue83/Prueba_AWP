package com.proyecto.servicios.security;

import com.proyecto.servicios.exception.NoAutenticadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Se ejecuta antes de los controladores protegidos: exige el encabezado
 * "Authorization: Bearer <token>" y guarda en la petición qué cliente la hace.
 */
@Component
public class AutenticacionInterceptor implements HandlerInterceptor {

    public static final String CLIENTE_AUTENTICADO = "clienteAutenticado";
    private static final String PREFIJO = "Bearer ";

    private final TokenService tokenService;

    public AutenticacionInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String ruta = request.getServletPath();
        if ("POST".equalsIgnoreCase(request.getMethod())
                && ("/clientes".equals(ruta) || "/clientes/".equals(ruta))) {
            return true;
        }
        String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (encabezado == null || !encabezado.startsWith(PREFIJO) || encabezado.length() == PREFIJO.length()) {
            throw new NoAutenticadoException("Falta el token de sesión. Inicia sesión en POST /login y envía Authorization: Bearer <token de sesión>");
        }
        Integer clienteId = tokenService.obtenerClienteId(encabezado.substring(PREFIJO.length()).trim());
        request.setAttribute(CLIENTE_AUTENTICADO, clienteId);
        return true;
    }
}