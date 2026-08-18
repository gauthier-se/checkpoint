package com.checkpoint.api.security.oauth2;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.checkpoint.api.entities.AuthProvider;
import com.checkpoint.api.entities.User;

/**
 * Spring Security {@link OAuth2UserService} for non-OIDC providers
 * (currently Twitch).
 *
 * <p>Twitch's Helix {@code /users} endpoint requires a {@code Client-Id} header
 * alongside the bearer token and wraps the user object inside a {@code data}
 * array, so the stock {@link DefaultOAuth2UserService} cannot be used as-is:
 * we issue the request manually instead.</p>
 *
 * <p>The returned principal carries the local user's email as its
 * {@link OAuth2User#getName()} so the success handler can resolve the
 * corresponding {@link User}.</p>
 */
@Service
public class CheckpointOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Logger log = LoggerFactory.getLogger(CheckpointOAuth2UserService.class);
    private static final String TWITCH_REGISTRATION_ID = "twitch";

    private final com.checkpoint.api.services.OAuth2UserService oAuth2UserService;
    private final RestClient restClient;

    public CheckpointOAuth2UserService(com.checkpoint.api.services.OAuth2UserService oAuth2UserService) {
        this.oAuth2UserService = oAuth2UserService;
        this.restClient = RestClient.create();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!TWITCH_REGISTRATION_ID.equalsIgnoreCase(registrationId)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"),
                    "Unsupported OAuth2 provider: " + registrationId);
        }

        String userInfoUri = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUri();
        String clientId = userRequest.getClientRegistration().getClientId();
        String accessToken = userRequest.getAccessToken().getTokenValue();

        Map<String, Object> attrs = fetchTwitchUser(userInfoUri, clientId, accessToken);

        String providerId = stringAttribute(attrs, "id");
        String email = stringAttribute(attrs, "email");
        String displayName = stringAttribute(attrs, "display_name");
        if (displayName == null) {
            displayName = stringAttribute(attrs, "login");
        }
        String picture = stringAttribute(attrs, "profile_image_url");

        if (providerId == null || email == null) {
            log.warn("Twitch OAuth2 response missing id or email");
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"),
                    "Twitch did not return the required id and email attributes");
        }

        User user = oAuth2UserService.loadOrCreateUser(
                AuthProvider.TWITCH, providerId, email, displayName, picture);

        String roleName = user.getRole() != null ? user.getRole().getName() : "USER";
        Map<String, Object> principalAttrs = new HashMap<>(attrs);
        principalAttrs.put("email", user.getEmail());
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase())),
                principalAttrs,
                "email");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchTwitchUser(String userInfoUri, String clientId, String accessToken) {
        Map<String, Object> response;
        try {
            response = restClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("Client-Id", clientId)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException(new OAuth2Error("user_info_request_failed"),
                    "Failed to fetch Twitch user info: " + ex.getMessage(), ex);
        }

        if (response == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("user_info_empty"),
                    "Twitch user-info response was empty");
        }
        Object data = response.get("data");
        if (!(data instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof Map<?, ?> first)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("user_info_malformed"),
                    "Twitch user-info response did not contain a data array");
        }
        return (Map<String, Object>) first;
    }

    private static String stringAttribute(Map<String, Object> attrs, String key) {
        Object value = attrs.get(key);
        return value != null ? value.toString() : null;
    }
}
