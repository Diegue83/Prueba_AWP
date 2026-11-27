package com.proyecto.servicios.config;

import com.proyecto.servicios.security.AutenticacionInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SeguridadWebConfig implements WebMvcConfigurer {

    private final AutenticacionInterceptor autenticacionInterceptor;
    private final String[] origenesPermitidos;

    public SeguridadWebConfig(AutenticacionInterceptor autenticacionInterceptor,
                              @Value("${app.cors.origenes:}") String[] origenesPermitidos) {
        this.autenticacionInterceptor = autenticacionInterceptor;
        this.origenesPermitidos = origenesPermitidos;
    }

    /** Solo los front web declarados en CORS_ORIGENES; la app móvil no usa CORS. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origenesPermitidos.length == 0) {
            return;
        }
        registry.addMapping("/**")
                .allowedOriginPatterns(origenesPermitidos)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Idempotency-Key")
                .maxAge(3600);
    }

    /** Protege clientes, cuentas, pagos y 2FA; el catálogo de productos es público y el interceptor exceptúa el POST de alta. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacionInterceptor)
                .addPathPatterns("/clientes", "/clientes/**", "/cuentas", "/cuentas/**",
                        "/pagos", "/pagos/**", "/2fa", "/2fa/**");
    }
}