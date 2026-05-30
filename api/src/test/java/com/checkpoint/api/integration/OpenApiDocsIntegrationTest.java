package com.checkpoint.api.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the SpringDoc OpenAPI specification.
 *
 * <p>Boots the full application context against an in-memory H2 database and
 * verifies that the generated {@code /v3/api-docs} document exposes the expected
 * metadata, JWT security schemes (so the Swagger UI shows an Authorize button)
 * and controller tag groups.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:openapitest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.search.backend.type=lucene",
        "spring.jpa.properties.hibernate.search.backend.directory.type=local-heap"
})
class OpenApiDocsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /v3/api-docs exposes API metadata, JWT security schemes and tags")
    void apiDocsExposesMetadataSecuritySchemesAndTags() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("CheckPoint API"))
                .andExpect(jsonPath("$.info.version").value("0.0.1"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.components.securitySchemes.cookie-auth.type").value("apiKey"))
                .andExpect(jsonPath("$.components.securitySchemes.cookie-auth.in").value("cookie"))
                .andExpect(jsonPath("$.components.securitySchemes.cookie-auth.name").value("checkpoint_token"))
                .andExpect(jsonPath("$.tags[*].name", hasItem("Games")));
    }

    @Test
    @DisplayName("GET /swagger-ui/index.html is served when SpringDoc is enabled")
    void swaggerUiIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
