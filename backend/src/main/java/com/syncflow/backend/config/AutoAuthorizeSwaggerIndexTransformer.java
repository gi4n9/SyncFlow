package com.syncflow.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Adds the login-response hook to Springdoc's generated Swagger UI initializer. */
public class AutoAuthorizeSwaggerIndexTransformer extends SwaggerIndexPageTransformer {
    private static final String INITIALIZER = "swagger-initializer.js";
    private static final String BUNDLE_INITIALIZATION = "window.ui = SwaggerUIBundle({";
    private static final String LOGIN_TOKEN_HOOK = """
        window.ui = SwaggerUIBundle({
          responseInterceptor: function(response) {
            if (response && response.status === 200 && response.url &&
                response.url.indexOf('/api/v1/auth/login') !== -1) {
              var body = response.body;
              if (typeof body === 'string') {
                try { body = JSON.parse(body); } catch (_) { body = null; }
              }
              if (body && body.accessToken && window.ui) {
                window.ui.preauthorizeApiKey('bearerAuth', body.accessToken);
              }
            }
            return response;
          },
        """;

    public AutoAuthorizeSwaggerIndexTransformer(SwaggerUiConfigProperties swaggerUiConfig,
                                                  SwaggerUiOAuthProperties swaggerUiOAuthProperties,
                                                  SwaggerWelcomeCommon swaggerWelcomeCommon,
                                                  ObjectMapperProvider objectMapperProvider) {
        super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
    }

    @Override
    public Resource transform(HttpServletRequest request, Resource resource,
                              ResourceTransformerChain transformerChain) throws IOException {
        Resource transformed = super.transform(request, resource, transformerChain);
        if (!resource.getURL().toString().contains(INITIALIZER)) return transformed;

        String initializer = StreamUtils.copyToString(transformed.getInputStream(), StandardCharsets.UTF_8);
        if (!initializer.contains(BUNDLE_INITIALIZATION)) return transformed;

        String customized = initializer.replace(BUNDLE_INITIALIZATION, LOGIN_TOKEN_HOOK);
        return new TransformedResource(resource, customized.getBytes(StandardCharsets.UTF_8));
    }
}
