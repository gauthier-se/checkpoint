package com.checkpoint.api.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import com.checkpoint.api.entities.Role;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.security.JwtService;

/**
 * Integration tests for dual security filter chain configuration.
 * Validates that the API chain (JWT + session hybrid) and the Web chain (session-based)
 * behave correctly for public and protected endpoints.
 *
 * Uses H2 in-memory database to avoid requiring Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:securitytest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.search.backend.type=lucene",
        "spring.jpa.properties.hibernate.search.backend.directory.type=local-heap"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeAll
    void setUpTestUser() {
        transactionTemplate.executeWithoutResult(status -> {
            Role adminRole = entityManager
                    .createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", "ADMIN")
                    .getResultStream()
                    .findFirst()
                    .orElseGet(() -> {
                        Role r = new Role("ADMIN");
                        entityManager.persist(r);
                        return r;
                    });

            com.checkpoint.api.entities.User testUser = userRepository.findByEmail("admin@test.com")
                    .orElseGet(() -> {
                        com.checkpoint.api.entities.User u = new com.checkpoint.api.entities.User();
                        u.setPseudo("admin");
                        u.setEmail("admin@test.com");
                        u.setPassword(passwordEncoder.encode("password"));
                        return u;
                    });

            testUser.setRole(adminRole);
            userRepository.save(testUser);
        });
    }

    @Nested
    @DisplayName("API Filter Chain (JWT + session hybrid)")
    class ApiFilterChainTests {

        @Test
        @DisplayName("GET /api/v1/games should be publicly accessible")
        void publicGamesList_shouldBeAccessible() throws Exception {
            mockMvc.perform(get("/api/v1/games"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/v1/games/{id} should be publicly accessible (returns 404 for missing game)")
        void publicGameDetails_shouldBeAccessible() throws Exception {
            mockMvc.perform(get("/api/v1/games/00000000-0000-0000-0000-000000000001"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/v1/admin/external-games/search should require authentication")
        void protectedAdminEndpoint_shouldRequireAuth() throws Exception {
            mockMvc.perform(get("/api/v1/admin/external-games/search")
                            .param("query", "zelda"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/v1/admin/games/import/{id} should require authentication")
        void protectedAdminImport_shouldRequireAuth() throws Exception {
            mockMvc.perform(post("/api/v1/admin/games/import/12345"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/v1/auth/** should be publicly accessible (not return 401)")
        void authEndpoints_shouldBePublic() throws Exception {
            // Auth endpoints don't exist yet, but the security chain should NOT block them
            mockMvc.perform(get("/api/v1/auth/test"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assert status != 401 : "Expected /api/v1/auth/** to NOT return 401, got " + status;
                        assert status != 403 : "Expected /api/v1/auth/** to NOT return 403, got " + status;
                    });
        }

        @Test
        @DisplayName("GET /api/v1/auth/me should require authentication")
        void meEndpoint_shouldRequireAuth() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/v1/auth/me should be accessible with valid JWT")
        void meEndpoint_withValidJwt_shouldBeAccessible() throws Exception {
            // Given
            UserDetails userDetails = User.builder()
                    .username("admin@test.com")
                    .password("password")
                    .roles("ADMIN")
                    .build();

            String token = jwtService.generateToken(Map.of(), userDetails);

            // When / Then
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Protected endpoint should be accessible with valid JWT")
        void protectedEndpoint_withValidJwt_shouldBeAccessible() throws Exception {
            // Given
            UserDetails userDetails = User.builder()
                    .username("admin@test.com")
                    .password("password")
                    .roles("ADMIN")
                    .build();

            String token = jwtService.generateToken(Map.of(), userDetails);

            // When / Then: assert authentication passes (not 401/403),
            // regardless of downstream service availability
            mockMvc.perform(get("/api/v1/admin/external-games/search")
                            .param("query", "zelda")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assert status != 401 : "Expected authenticated request to NOT return 401, got 401";
                        assert status != 403 : "Expected authenticated request to NOT return 403, got 403";
                    });
        }

        @Test
        @DisplayName("Protected endpoint should reject invalid JWT")
        void protectedEndpoint_withInvalidJwt_shouldReject() throws Exception {
            mockMvc.perform(get("/api/v1/admin/external-games/search")
                            .param("query", "zelda")
                            .header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Web Filter Chain (session-based)")
    class WebFilterChainTests {

        @Test
        @DisplayName("/login should be publicly accessible")
        void loginPage_shouldBeAccessible() throws Exception {
            // Default Spring Security login page should be accessible
            mockMvc.perform(get("/login"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assert status != 401 : "Expected /login to NOT return 401, got " + status;
                        assert status != 403 : "Expected /login to NOT return 403, got " + status;
                    });
        }

        @Test
        @DisplayName("/error should be publicly accessible")
        void errorEndpoint_shouldBeAccessible() throws Exception {
            mockMvc.perform(get("/error"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assert status != 401 : "Expected /error to NOT return 401, got " + status;
                        assert status != 403 : "Expected /error to NOT return 403, got " + status;
                    });
        }
    }

    @Nested
    @DisplayName("Filter Chain Priority")
    class FilterChainPriorityTests {

        @Test
        @DisplayName("API chain should return 401 for unauthenticated requests, not redirect")
        void apiChain_shouldReturn401NotRedirect() throws Exception {
            // API requests without credentials should return 401, not redirect to login
            mockMvc.perform(get("/api/v1/admin/external-games/search")
                            .param("query", "zelda"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
