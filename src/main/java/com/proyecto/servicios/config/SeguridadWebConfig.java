package com.proyecto.servicios.config;

import com.proyecto.servicios.security.AutenticacionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SeguridadWebConfig implements WebMvcConfigurer {

    private final AutenticacionInterceptor autenticacionInterceptor;

    public SeguridadWebConfig(AutenticacionInterceptor autenticacionInterceptor) {
        this.autenticacionInterceptor = autenticacionInterceptor;
    }

    /** Protege los recursos de clientes, cuentas y productos; el interceptor exceptúa el POST de alta. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacionInterceptor)
                .addPathPatterns("/clientes", "/clientes/**", "/cuentas", "/cuentas/**", "/productos", "/productos/**");
    }
}