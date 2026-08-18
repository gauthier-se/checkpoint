package com.checkpoint.api.integration;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the two actuator endpoints the CD workflow polls.
 *
 * <p>The deployment pipeline has no credentials and reads exactly two things: that
 * the service answers, and the build timestamp baked into the jar, which is what
 * distinguishes a container running a fresh image from one that never restarted.
 * Both are load-bearing, and neither is exercised by any other test, so a change
 * to the actuator base path, the exposure list or the security rules would
 * otherwise only be noticed by a deployment hanging for fifteen minutes.
 *
 * <p>Note the {@code /api/v1} prefix: it comes from
 * {@code management.endpoints.web.base-path}, not from the
 * {@link com.checkpoint.api.config.WebConfig} path prefix, which only applies to
 * controllers. The web app's Nitro proxy forwards {@code /api/**} and nothing
 * else, so an actuator sitting anywhere else is unreachable from the internet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:deployprobetest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.search.backend.type=lucene",
        "spring.jpa.properties.hibernate.search.backend.directory.type=local-heap"
})
class DeployProbeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/actuator/health is public and reports UP without details")
    void healthIsPublicAndTerse() throws Exception {
        mockMvc.perform(get("/api/v1/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                // show-details=never: the endpoint is reachable from the internet,
                // so it must not enumerate the datastores behind it.
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/actuator/info is public and exposes the build timestamp")
    void infoIsPublicAndCarriesTheBuildStamp() throws Exception {
        mockMvc.perform(get("/api/v1/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.build.artifact").value("api"))
                // The CD workflow parses this with `date -d`, so it has to stay an
                // ISO-8601 instant. It is written by the spring-boot-maven-plugin
                // build-info goal: running the tests outside the Maven lifecycle
                // leaves it absent and fails here, which is intended.
                .andExpect(jsonPath("$.build.time")
                        .value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z")))
                // Nothing beyond the build block: no environment, no JVM, no OS.
                .andExpect(jsonPath("$.java").doesNotExist())
                .andExpect(jsonPath("$.os").doesNotExist());
    }

    @Test
    @DisplayName("Actuator endpoints other than health and info reject anonymous callers")
    void otherEndpointsAreNotPublic() throws Exception {
        // Only health and info are listed in the security config, so everything
        // else falls through to anyRequest().authenticated() and is refused before
        // the handler is ever looked up: hence 401 rather than 404.
        mockMvc.perform(get("/api/v1/actuator/env"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/actuator/beans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Actuator endpoints other than health and info are not exposed at all")
    void otherEndpointsAreNotExposed() throws Exception {
        // Authenticated, so security lets the request through: a 404 here proves the
        // endpoints are absent from management.endpoints.web.exposure.include rather
        // than merely guarded. Widening that list would turn these into 200s.
        mockMvc.perform(get("/api/v1/actuator/env"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/actuator/beans"))
                .andExpect(status().isNotFound());
    }
}
