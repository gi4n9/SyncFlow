package com.syncflow.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    private static final String JWT_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI syncFlowOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SyncFlow API")
                .version("v1")
                .description("Workspace and Kanban API. Use an access token from POST /api/v1/auth/login."))
            .components(new Components().addSecuritySchemes(JWT_SCHEME,
                new SecurityScheme()
                    .name(JWT_SCHEME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Paste the accessToken value returned by POST /api/v1/auth/login.")))
            .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME));
    }

    @Bean
    public SwaggerIndexTransformer swaggerIndexTransformer(SwaggerUiConfigProperties swaggerUiConfig,
                                                             SwaggerUiOAuthProperties swaggerUiOAuthProperties,
                                                             SwaggerWelcomeCommon swaggerWelcomeCommon,
                                                             ObjectMapperProvider objectMapperProvider) {
        return new AutoAuthorizeSwaggerIndexTransformer(swaggerUiConfig, swaggerUiOAuthProperties,
            swaggerWelcomeCommon, objectMapperProvider);
    }
}
