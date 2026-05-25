package com.checkpoint.api.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.util.HtmlUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.checkpoint.api.client.SteamApiClient;
import com.checkpoint.api.client.SteamOpenIdClient;
import com.checkpoint.api.dto.steam.SteamPlayerSummaryDto;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.exceptions.InvalidTokenException;
import com.checkpoint.api.exceptions.SteamApiException;
import com.checkpoint.api.exceptions.SteamOpenIdException;
import com.checkpoint.api.services.SteamOpenIdStateService;
import com.checkpoint.api.services.SteamService;
import com.checkpoint.api.services.SteamSignupTokenService;

import jakarta.servlet.http.HttpServletRequest;

import com.checkpoint.api.dto.auth.AuthMessageDto;
import com.checkpoint.api.dto.auth.ForgotPasswordRequestDto;
import com.checkpoint.api.dto.auth.LoginRequestDto;
import com.checkpoint.api.dto.auth.LoginResponseDto;
import com.checkpoint.api.dto.auth.RefreshTokenRequestDto;
import com.checkpoint.api.dto.auth.RegisterRequestDto;
import com.checkpoint.api.dto.auth.RegisterWithSteamRequestDto;
import com.checkpoint.api.dto.auth.ResetPasswordRequestDto;
import com.checkpoint.api.dto.auth.SteamSignupPrefillDto;
import com.checkpoint.api.dto.auth.TokenPairDto;
import com.checkpoint.api.dto.auth.TwoFactorDisableRequestDto;
import com.checkpoint.api.dto.auth.TwoFactorLoginRequestDto;
import com.checkpoint.api.dto.auth.TwoFactorRequiredResponseDto;
import com.checkpoint.api.dto.auth.TwoFactorSetupResponseDto;
import com.checkpoint.api.dto.auth.TwoFactorVerifyRequestDto;
import com.checkpoint.api.dto.auth.UserMeDto;
import com.checkpoint.api.exceptions.TwoFactorRequiredException;
import com.checkpoint.api.services.AuthService;
import com.checkpoint.api.services.TwoFactorService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Authentication controller supporting two client types:
 *
 * <ul>
 *   <li><strong>Desktop</strong> ({@code X-Client-Type: Desktop} header or
 *       {@code POST /api/auth/token}): returns a JWT access token and a refresh token in the response body.</li>
 *   <li><strong>Web</strong> (default): sets {@code checkpoint_token} (access) and
 *       {@code checkpoint_refresh} (refresh) HttpOnly cookies.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final String STEAM_ACTION_LOGIN = "login";
    private static final String STEAM_ACTION_LINK = "link";
    private static final String STEAM_ACTION_SIGNUP = "signup";

    private final AuthService authService;
    private final TwoFactorService twoFactorService;
    private final SteamOpenIdClient steamOpenIdClient;
    private final SteamService steamService;
    private final SteamOpenIdStateService steamOpenIdStateService;
    private final SteamSignupTokenService steamSignupTokenService;
    private final SteamApiClient steamApiClient;

    @Value("${steam.openid.return-url:http://localhost:8080/api/auth/steam/openid/callback}")
    private String steamReturnUrl;

    @Value("${steam.openid.realm:http://localhost:8080}")
    private String steamRealm;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public AuthController(AuthService authService,
                          TwoFactorService twoFactorService,
                          SteamOpenIdClient steamOpenIdClient,
                          SteamService steamService,
                          SteamOpenIdStateService steamOpenIdStateService,
                          SteamSignupTokenService steamSignupTokenService,
                          SteamApiClient steamApiClient) {
        this.authService = authService;
        this.twoFactorService = twoFactorService;
        this.steamOpenIdClient = steamOpenIdClient;
        this.steamService = steamService;
        this.steamOpenIdStateService = steamOpenIdStateService;
        this.steamSignupTokenService = steamSignupTokenService;
        this.steamApiClient = steamApiClient;
    }

    /**
     * Unified login endpoint.
     *
     * <p>Desktop clients (identified by the {@code X-Client-Type: Desktop} header) receive a
     * {@link TokenPairDto} in the response body. Web clients receive both cookies.</p>
     *
     * <p>If the user has 2FA enabled, a {@link TwoFactorRequiredResponseDto} is returned instead.
     * Desktop clients receive the intermediate token in the body; Web clients receive it via the
     * {@code checkpoint_2fa} HttpOnly cookie.</p>
     *
     * @param loginRequest    the login credentials
     * @param clientType      optional header to specify the client type
     * @param servletResponse the HTTP servlet response (used to write cookies for Web clients)
     * @return token pair (Desktop), success message (Web), or 2FA required response
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequestDto loginRequest,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            HttpServletResponse servletResponse) {

        try {
            if ("Desktop".equalsIgnoreCase(clientType)) {
                TokenPairDto pair = authService.authenticateAndGenerateTokenPair(loginRequest);
                return ResponseEntity.ok(pair);
            }

            authService.authenticateAndSetCookie(loginRequest, servletResponse);
            return ResponseEntity.ok(new AuthMessageDto("Login successful"));
        } catch (TwoFactorRequiredException ex) {
            return ResponseEntity.ok(new TwoFactorRequiredResponseDto(true, ex.getIntermediateToken()));
        }
    }

    /**
     * Initiates TOTP setup for the authenticated user.
     * Generates a new TOTP secret, stores it on the user, and returns a QR code data URL
     * plus the provisioning URI. 2FA is not yet active until {@code /2fa/verify} is called.
     *
     * @param userDetails the authenticated user principal
     * @return setup response with QR code and provisioning URI
     */
    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorSetupResponseDto> twoFactorSetup(
            @AuthenticationPrincipal UserDetails userDetails) {
        TwoFactorSetupResponseDto response = twoFactorService.setup(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Verifies the first TOTP code after setup and permanently enables 2FA on the account.
     *
     * @param request     the verification request containing the 6-digit code
     * @param userDetails the authenticated user principal
     * @return success message
     */
    @PostMapping("/2fa/verify")
    public ResponseEntity<AuthMessageDto> twoFactorVerify(
            @Valid @RequestBody TwoFactorVerifyRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {
        twoFactorService.verifyAndEnable(userDetails.getUsername(), request.code());
        return ResponseEntity.ok(new AuthMessageDto("Two-factor authentication has been enabled."));
    }

    /**
     * Disables 2FA on the account after verifying the current password and a valid TOTP code.
     *
     * @param request     the disable request containing the current password and TOTP code
     * @param userDetails the authenticated user principal
     * @return success message
     */
    @PostMapping("/2fa/disable")
    public ResponseEntity<AuthMessageDto> twoFactorDisable(
            @Valid @RequestBody TwoFactorDisableRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {
        twoFactorService.disable(userDetails.getUsername(), request.password(), request.code());
        return ResponseEntity.ok(new AuthMessageDto("Two-factor authentication has been disabled."));
    }

    /**
     * Completes the 2FA login step.
     *
     * <p>Web clients send only the TOTP code in the request body; the intermediate token is
     * read from the {@code checkpoint_2fa} HttpOnly cookie. On success, the final
     * {@code checkpoint_token} and {@code checkpoint_refresh} cookies are set.</p>
     *
     * <p>Desktop clients ({@code X-Client-Type: Desktop}) include both the intermediate token
     * and the TOTP code in the request body. On success, a {@link TokenPairDto} is returned.</p>
     *
     * @param request         the 2FA login request
     * @param clientType      optional header to identify Desktop clients
     * @param twoFaCookie     the {@code checkpoint_2fa} cookie value (Web clients)
     * @param servletResponse the HTTP servlet response to write the final auth cookies on
     * @return token pair (Desktop) or success message (Web)
     */
    @PostMapping("/2fa/login")
    public ResponseEntity<?> twoFactorLogin(
            @Valid @RequestBody TwoFactorLoginRequestDto request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @CookieValue(value = "checkpoint_2fa", required = false) String twoFaCookie,
            HttpServletResponse servletResponse) {

        if ("Desktop".equalsIgnoreCase(clientType)) {
            TokenPairDto pair = authService.completeTwoFactorLoginForDesktop(request);
            return ResponseEntity.ok(pair);
        }

        authService.completeTwoFactorLoginForWeb(request, twoFaCookie, servletResponse);
        return ResponseEntity.ok(new AuthMessageDto("Login successful"));
    }

    /**
     * Endpoint for user registration.
     *
     * @param registerRequest the registration details
     * @return 201 Created on success
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequestDto registerRequest) {

        authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Dedicated token endpoint for Desktop clients.
     * Returns a {@link TokenPairDto} containing both the access and refresh tokens.
     *
     * @param loginRequest the login credentials
     * @return token pair in the response body
     */
    @PostMapping("/token")
    public ResponseEntity<TokenPairDto> token(
            @Valid @RequestBody LoginRequestDto loginRequest) {

        TokenPairDto pair = authService.authenticateAndGenerateTokenPair(loginRequest);
        return ResponseEntity.ok(pair);
    }

    /**
     * Token refresh endpoint.
     *
     * <p>Web clients send the {@code checkpoint_refresh} cookie; the response sets new
     * {@code checkpoint_token} and {@code checkpoint_refresh} cookies (token rotation).</p>
     *
     * <p>Desktop clients ({@code X-Client-Type: Desktop}) send the refresh token in the
     * request body and receive a new {@link TokenPairDto}.</p>
     *
     * @param clientType      optional header to identify Desktop clients
     * @param refreshCookie   the {@code checkpoint_refresh} cookie value (Web clients)
     * @param body            the refresh token request body (Desktop clients)
     * @param servletResponse the HTTP servlet response to write new cookies on (Web clients)
     * @return token pair (Desktop) or success message (Web)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @CookieValue(value = "checkpoint_refresh", required = false) String refreshCookie,
            @RequestBody(required = false) RefreshTokenRequestDto body,
            HttpServletResponse servletResponse) {

        if ("Desktop".equalsIgnoreCase(clientType)) {
            String token = (body != null) ? body.refreshToken() : null;
            TokenPairDto pair = authService.refreshTokenForDesktop(token);
            return ResponseEntity.ok(pair);
        }

        authService.refreshTokenAndSetCookie(refreshCookie, servletResponse);
        return ResponseEntity.ok(new AuthMessageDto("Token refreshed"));
    }

    /**
     * Logout endpoint.
     *
     * <p>Revokes the refresh token, expires both cookies, and clears the security context.</p>
     *
     * @param refreshCookie   the {@code checkpoint_refresh} cookie value, or {@code null}
     * @param servletResponse the HTTP servlet response to write expired cookies on
     * @return success message
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthMessageDto> logout(
            @CookieValue(value = "checkpoint_refresh", required = false) String refreshCookie,
            HttpServletResponse servletResponse) {
        authService.clearAuthCookie(refreshCookie, servletResponse);
        return ResponseEntity.ok(new AuthMessageDto("Logout successful"));
    }

    /**
     * Generates a short-lived JWT for WebSocket authentication.
     *
     * <p>Web clients authenticated via the {@code checkpoint_token} cookie can call
     * this endpoint to obtain a JWT for the STOMP WebSocket connection.</p>
     *
     * @param userDetails the authenticated user principal (injected by Spring Security)
     * @return JWT token in the response body
     */
    @GetMapping("/ws-token")
    public ResponseEntity<LoginResponseDto> wsToken(
            @AuthenticationPrincipal UserDetails userDetails) {

        String token = authService.generateWsToken(userDetails);
        return ResponseEntity.ok(new LoginResponseDto(token));
    }

    /**
     * Returns profile information for the currently authenticated user.
     *
     * @param userDetails the authenticated user principal (injected by Spring Security)
     * @return user profile including ID, username, email, and role
     */
    @GetMapping("/me")
    public ResponseEntity<UserMeDto> me(@AuthenticationPrincipal UserDetails userDetails) {
        UserMeDto user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    /**
     * Endpoint for requesting a password reset.
     *
     * @param request the forgot password request
     * @return 200 OK
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthMessageDto> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request) {

        authService.forgotPassword(request);
        return ResponseEntity.ok(new AuthMessageDto("If the email exists, a password reset link has been logged."));
    }

    /**
     * Endpoint for resetting a password.
     *
     * @param request the reset password request
     * @return 200 OK
     */
    @PostMapping("/reset-password")
    public ResponseEntity<AuthMessageDto> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(new AuthMessageDto("Password has been reset successfully."));
    }

    /**
     * Initiates the Steam OpenID 2.0 flow. The user is redirected to Steam to authenticate;
     * Steam then redirects back to {@code /api/auth/steam/openid/callback} with the {@code action}
     * preserved in the query string.
     *
     * @param action {@code login} (issue a session for an existing linked account),
     *               {@code link} (attach the SteamID to the currently authenticated user), or
     *               {@code signup} (open the prefilled signup form when no account is linked)
     * @return 302 redirect to Steam
     */
    @GetMapping("/steam/openid/start")
    public ResponseEntity<?> steamOpenIdStart(
            @RequestParam(defaultValue = STEAM_ACTION_LOGIN) String action,
            @AuthenticationPrincipal UserDetails userDetails) {
        String normalizedAction = switch (action) {
            case STEAM_ACTION_LINK -> STEAM_ACTION_LINK;
            case STEAM_ACTION_SIGNUP -> STEAM_ACTION_SIGNUP;
            default -> STEAM_ACTION_LOGIN;
        };

        String stateEmail = STEAM_ACTION_LINK.equals(normalizedAction) && userDetails != null
                ? userDetails.getUsername()
                : null;
        String state = steamOpenIdStateService.issue(normalizedAction, stateEmail);

        String returnUrl = steamReturnUrl
                + "?action=" + normalizedAction
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
        String authUrl = steamOpenIdClient.buildAuthenticationUrl(returnUrl, steamRealm);

        log.info("Redirecting to Steam OpenID for action={}", normalizedAction);
        return clientSideRedirect(authUrl);
    }

    /**
     * Steam OpenID 2.0 callback. Verifies the response with Steam, extracts the SteamID64, then:
     * <ul>
     *   <li>{@code action=login} or {@code action=signup}: looks up the user with this SteamID.
     *       If found, establishes a web session (sets auth cookies) and redirects to the frontend
     *       root. If not found, issues a short-lived Steam signup token and redirects to
     *       {@code /register?steam_token=<jwt>} with the Steam display name and avatar prefilled.</li>
     *   <li>{@code action=link}: attaches the SteamID to the currently authenticated user and
     *       redirects to {@code /settings/integrations}.</li>
     * </ul>
     *
     * @param action          the action encoded in the return URL
     * @param request         the HTTP servlet request (used to enumerate all {@code openid.*} params)
     * @param userDetails     the authenticated user (for the link action)
     * @param servletResponse the HTTP servlet response (used to write auth cookies on login)
     * @return 302 redirect to the frontend
     */
    @GetMapping("/steam/openid/callback")
    public ResponseEntity<?> steamOpenIdCallback(
            @RequestParam(defaultValue = STEAM_ACTION_LOGIN) String action,
            @RequestParam(required = false) String state,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            jakarta.servlet.http.HttpServletResponse servletResponse) {

        Optional<SteamOpenIdStateService.Claims> stateClaims = steamOpenIdStateService.verify(state);
        if (stateClaims.isEmpty()) {
            log.warn("Steam OpenID callback rejected: missing or invalid state token (action={})", action);
            return clientSideRedirect(frontendUrl + "/login?error=steam_openid_failed");
        }
        if (!stateClaims.get().action().equals(action)) {
            log.warn("Steam OpenID callback rejected: state action {} != URL action {}",
                    stateClaims.get().action(), action);
            return clientSideRedirect(frontendUrl + "/login?error=steam_openid_failed");
        }
        if (STEAM_ACTION_LINK.equals(action)) {
            String stateEmail = stateClaims.get().email();
            if (userDetails == null || stateEmail == null || !stateEmail.equals(userDetails.getUsername())) {
                log.warn("Steam OpenID link callback rejected: state user does not match authenticated user");
                return clientSideRedirect(frontendUrl + "/login?error=steam_openid_failed");
            }
        }

        Map<String, String> openIdParams = collectOpenIdParams(request);

        String steamId;
        try {
            steamId = steamOpenIdClient.verifyAndExtractSteamId(openIdParams);
        } catch (SteamOpenIdException e) {
            log.warn("Steam OpenID callback verification failed: {}", e.getMessage());
            return clientSideRedirect(frontendUrl + "/login?error=steam_openid_failed");
        }

        if (STEAM_ACTION_LINK.equals(action)) {
            steamService.linkVerifiedSteamAccount(userDetails.getUsername(), steamId);
            log.info("Linked Steam {} to user {} via OpenID", steamId, userDetails.getUsername());
            return clientSideRedirect(frontendUrl + "/settings/integrations?linked=steam");
        }

        // login / signup: try to log in the existing user, otherwise hand off to the prefilled signup.
        User user = steamService.findUserBySteamId(steamId).orElse(null);
        if (user != null) {
            if (authService.requireTwoFactorChallenge(user.getEmail(), servletResponse)) {
                log.info("Steam OpenID login requires 2FA challenge for {}", user.getEmail());
                return clientSideRedirect(frontendUrl + "/login?2fa=required");
            }
            authService.establishWebSession(user.getEmail(), servletResponse);
            log.info("Steam OpenID login successful for {}", user.getEmail());
            return clientSideRedirect(frontendUrl + "/");
        }

        log.info("Steam OpenID: no user linked to SteamID {}, issuing signup token", steamId);
        return clientSideRedirect(frontendUrl + "/register?steam_token=" + buildSteamSignupToken(steamId));
    }

    /**
     * Returns the Steam identity carried by a verified signup token, so the {@code /register}
     * page can prefill the signup form. Idempotent and read-only.
     *
     * @param token the signed JWT from the {@code steam_token} query parameter
     * @return the prefill payload (steam id, display name, avatar URL, profile URL)
     */
    @GetMapping("/steam/signup-prefill")
    public ResponseEntity<SteamSignupPrefillDto> steamSignupPrefill(@RequestParam String token) {
        SteamSignupTokenService.Claims claims = steamSignupTokenService.verify(token)
                .orElseThrow(() -> new InvalidTokenException("Steam signup token is invalid or expired"));
        return ResponseEntity.ok(new SteamSignupPrefillDto(
                claims.steamId(),
                claims.steamDisplayName(),
                claims.steamAvatarUrl(),
                claims.steamProfileUrl()));
    }

    /**
     * Creates a new account from a verified Steam signup token, with optional password,
     * and establishes a web session (sets the auth cookies).
     *
     * @param request         the Steam-prefilled registration details
     * @param servletResponse the HTTP servlet response to write the auth cookies on
     * @return 201 Created with a confirmation message
     */
    @PostMapping("/register/steam")
    public ResponseEntity<AuthMessageDto> registerWithSteam(
            @Valid @RequestBody RegisterWithSteamRequestDto request,
            HttpServletResponse servletResponse) {
        authService.registerWithSteam(request, servletResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthMessageDto("Registration successful"));
    }

    /**
     * Mints a Steam signup token, best-effort enriching it with the Steam profile fields.
     * Failures to reach the Steam Web API are non-fatal: only the verified SteamID is required.
     */
    private String buildSteamSignupToken(String steamId) {
        String displayName = null;
        String avatarUrl = null;
        String profileUrl = null;
        try {
            SteamPlayerSummaryDto summary = steamApiClient.fetchPlayerSummary(steamId).orElse(null);
            if (summary != null) {
                displayName = summary.personaName();
                avatarUrl = summary.avatarMedium();
                profileUrl = summary.profileUrl();
            }
        } catch (SteamApiException e) {
            log.warn("Could not fetch Steam profile for signup prefill ({}): {}", steamId, e.getMessage());
        }
        return steamSignupTokenService.issue(steamId, displayName, avatarUrl, profileUrl);
    }

    private static Map<String, String> collectOpenIdParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("openid.") && values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }

    /**
     * Returns a 200 OK HTML page that triggers a client-side navigation to {@code target}.
     *
     * <p>HTTP 302 redirects break when the request is proxied by the dev-mode Nitro server
     * (Nitro follows redirects internally, which swallows {@code Set-Cookie} headers and lands
     * the browser on the callback URL with an unrelated body). Emitting HTML keeps the proxy
     * neutral: it's just a 200 response that's forwarded verbatim, cookies included.</p>
     *
     * @param target the absolute URL to navigate to
     * @return the HTML response
     */
    private static ResponseEntity<String> clientSideRedirect(String target) {
        String escapedHref = HtmlUtils.htmlEscape(target);
        // Use replace() so the redirect URL isn't kept in history.
        String html = "<!doctype html><html><head><meta charset=\"utf-8\">" +
                "<meta http-equiv=\"refresh\" content=\"0;url=" + escapedHref + "\">" +
                "<title>Redirecting…</title></head><body>" +
                "<script>window.location.replace(" +
                jsString(target) + ");</script>" +
                "<noscript>If you are not redirected automatically, <a href=\"" +
                escapedHref + "\">click here</a>.</noscript>" +
                "</body></html>";
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private static String jsString(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '<' -> sb.append("\\u003c");
                case '>' -> sb.append("\\u003e");
                case '&' -> sb.append("\\u0026");
                default -> sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
