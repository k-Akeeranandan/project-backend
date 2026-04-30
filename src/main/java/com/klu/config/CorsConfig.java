package com.klu.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
    private List<String> allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer(){
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry){
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins.toArray(String[]::new))
                        .allowedMethods("*")
                        .allowedHeaders("*");
            }
        };
    }
}
