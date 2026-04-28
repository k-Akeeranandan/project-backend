package com.klu.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI virtualCareerFairOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Virtual Career Fair API")
                        .version("1.0")
                        .description("REST API for virtual career fairs and networking. "
                                + "Authenticate via /auth/login, then use the Bearer token for protected routes.")
                        .contact(new Contact().name("KLU").email("support@example.com")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT returned by POST /auth/login")));
    }
}
