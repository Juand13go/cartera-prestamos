package io.github.juand13go.cartera.adaptadores.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra el interceptor de roles para todas las rutas, menos las públicas. */
@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final InterceptorRoles interceptorRoles;

    public ConfiguracionWeb(InterceptorRoles interceptorRoles) {
        this.interceptorRoles = interceptorRoles;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(interceptorRoles)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/login/**", "/css/**", "/error");
    }
}
