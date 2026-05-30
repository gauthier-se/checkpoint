package com.checkpoint.api.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * SpringDoc OpenAPI configuration for the CheckPoint API.
 *
 * <p>Defines the global API metadata, the available servers and two JWT-based
 * security schemes so the Swagger UI exposes an <em>Authorize</em> button:
 * <ul>
 *   <li>{@code bearer-jwt} — {@code Authorization: Bearer <token>} header (Desktop clients).</li>
 *   <li>{@code cookie-auth} — {@code checkpoint_token} HttpOnly cookie (Web clients).</li>
 * </ul>
 *
 * <p>The endpoints themselves ({@code /swagger-ui.html}, {@code /v3/api-docs}) are
 * toggled with the {@code SWAGGER_ENABLED} environment variable (see
 * {@code application.properties}); they are disabled in production.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";
    private static final String COOKIE_SCHEME = "cookie-auth";

    @Bean
    public OpenAPI checkpointOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CheckPoint API")
                        .version("0.0.1")
                        .description("REST API for CheckPoint — a video game library tracker. "
                                + "Authenticate with a JWT via the Authorization header (Desktop) "
                                + "or the checkpoint_token cookie (Web)."))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("https://checkpoint.seyzeriat.com").description("Production")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT supplied via the Authorization header (Desktop clients)."))
                        .addSecuritySchemes(COOKIE_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("checkpoint_token")
                                .description("JWT supplied via the checkpoint_token HttpOnly cookie (Web clients).")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
