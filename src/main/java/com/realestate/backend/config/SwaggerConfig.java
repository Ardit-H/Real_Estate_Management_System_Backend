package com.realestate.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


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
                // Shto JWT security requirement globalisht
                .addSecurityItem(
                        new SecurityRequirement().addList(SECURITY_SCHEME_NAME)
                )
                // Defino JWT Bearer scheme
                .components(
                        new Components().addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Vendos JWT token-in e marrë nga POST /api/auth/login.\n\n" +
                                                        "Format: **Bearer &lt;token&gt;**\n\n" +
                                                        "Token përmban: userId, tenantId, schemaName, role"
                                        )
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Real Estate Management System API")
                .version("1.0.0")
                .description(
                        "REST API për menaxhimin e pronave të paluajtshme.\n\n" +
                                "## Autentikim\n" +
                                "Sistemi përdor JWT (JSON Web Tokens).\n" +
                                "1. Regjistrohu: `POST /api/auth/register`\n" +
                                "2. Kyçu: `POST /api/auth/login`\n" +
                                "3. Merr `access_token` nga response\n" +
                                "4. Kliko 'Authorize' dhe vendos tokenin\n\n" +
                                "## Multitenancy\n" +
                                "Çdo kompani ka skemën e vet PostgreSQL.\n" +
                                "Tenant-i identifikohet automatikisht nga JWT."
                )
                .contact(new Contact()
                        .name("Real Estate API Team")
                        .email("api@realestate.com")
                )
                .license(new License()
                        .name("MIT")
                );
    }
}