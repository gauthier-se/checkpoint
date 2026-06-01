package com.checkpoint.api.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.checkpoint.api.security.JwtService;
import com.checkpoint.api.services.AuthService;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2AuthenticationSuccessHandler")
class OAuth2AuthenticationSuccessHandlerTest {

    private static final String FRONTEND_URL = "http://localhost:3000";
    private static final String EMAIL = "user@test.com";

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthService authService;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationSuccessHandler(
                userDetailsService, jwtService, authService,
                FRONTEND_URL, false, "", 86400000L);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("email", EMAIL));
    }

    @Test
    @DisplayName("Should redirect to the 2FA challenge and issue no session token when 2FA is required")
    void shouldRedirectToTwoFactorChallenge() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authService.requireTwoFactorChallenge(eq(EMAIL), any())).thenReturn(true);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo(FRONTEND_URL + "/login?2fa=required");
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
        verify(jwtService, never()).generateToken(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("Should set the checkpoint_token cookie and redirect to root when 2FA is not required")
    void shouldEstablishSessionWhenNoTwoFactor() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(EMAIL)
                .password("x")
                .roles("USER")
                .build();

        when(authService.requireTwoFactorChallenge(eq(EMAIL), any())).thenReturn(false);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt.token.here");

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getHeaders("Set-Cookie"))
                .anyMatch(h -> h.contains("checkpoint_token=jwt.token.here")
                        && h.contains("HttpOnly")
                        && h.contains("Path=/api/v1"));
        assertThat(response.getRedirectedUrl()).isEqualTo(FRONTEND_URL + "/");
    }
}
