package com.realestate.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SwaggerConfig — OpenAPI / Swagger UI configuration.
 *
 * Configures the interactive API documentation available at /swagger-ui.html.
 * Sets up JWT Bearer authentication so every endpoint can be tested directly
 * from the browser without needing an external tool like Postman.
 *
 * To authenticate in Swagger UI:
 *   1. Call POST /api/auth/login
 *   2. Copy the access_token from the response
 *   3. Click "Authorize" and paste the token
 *   4. All subsequent requests will include the Bearer header automatically
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development"),
                        new Server().url("https://api.yourdomain.com").description("Production")
                ))
                .addSecurityItem(
                        new SecurityRequirement().addList(SECURITY_SCHEME_NAME)
                )
                .components(
                        new Components().addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Paste the JWT token obtained from POST /api/auth/login.\n\n" +
                                                        "Format: **Bearer &lt;token&gt;**\n\n" +
                                                        "Token contains: userId, tenantId, schemaName, role"
                                        )
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Real Estate Management System API")
                .version("1.0.0")
                .description(
                        "REST API for real estate property management.\n\n" +
                                "## Authentication\n" +
                                "This API uses JWT (JSON Web Tokens) for stateless authentication.\n" +
                                "1. Register: `POST /api/auth/register`\n" +
                                "2. Login: `POST /api/auth/login`\n" +
                                "3. Copy the `access_token` from the response\n" +
                                "4. Click **Authorize** and paste the token\n" +
                                "5. All subsequent requests will include the Bearer header automatically\n\n" +
                                "## Multi-tenancy\n" +
                                "Each company gets its own isolated PostgreSQL schema.\n" +
                                "The tenant is identified automatically from the JWT on every request.\n\n" +
                                "## Authorization\n" +
                                "Access control is permission-based. Each role (ADMIN, AGENT, CLIENT)\n" +
                                "has a set of allowed HTTP method + path combinations stored in the database.\n" +
                                "Unauthorized requests return 403 Forbidden."
                );
    }
}