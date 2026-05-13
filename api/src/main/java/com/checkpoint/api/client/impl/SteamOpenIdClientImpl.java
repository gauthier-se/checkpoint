package com.checkpoint.api.client.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.checkpoint.api.client.SteamOpenIdClient;
import com.checkpoint.api.exceptions.SteamOpenIdException;

/**
 * Implementation of {@link SteamOpenIdClient} that talks directly to Steam's OpenID 2.0 endpoint.
 *
 * <p>Spring Security has no OpenID 2.0 support (the spec is officially deprecated in 2014 but
 * Steam is one of the few remaining major providers), so the verification handshake is performed
 * here with a plain {@link RestClient}.</p>
 */
@Component
public class SteamOpenIdClientImpl implements SteamOpenIdClient {

    private static final Logger log = LoggerFactory.getLogger(SteamOpenIdClientImpl.class);

    private static final String OPENID_NS = "http://specs.openid.net/auth/2.0";
    private static final String OPENID_IDENTIFIER_SELECT = "http://specs.openid.net/auth/2.0/identifier_select";
    private static final Pattern CLAIMED_ID_PATTERN =
            Pattern.compile("^https?://steamcommunity\\.com/openid/id/(\\d{17})$");

    private final String providerUrl;
    private final RestClient restClient;

    public SteamOpenIdClientImpl(@Value("${steam.openid.provider-url}") String providerUrl) {
        this.providerUrl = providerUrl;
        this.restClient = RestClient.create();
    }

    @Override
    public String buildAuthenticationUrl(String returnUrl, String realm) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("openid.ns", OPENID_NS);
        params.put("openid.mode", "checkid_setup");
        params.put("openid.return_to", returnUrl);
        params.put("openid.realm", realm);
        params.put("openid.identity", OPENID_IDENTIFIER_SELECT);
        params.put("openid.claimed_id", OPENID_IDENTIFIER_SELECT);

        StringBuilder url = new StringBuilder(providerUrl);
        url.append('?');
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                url.append('&');
            }
            url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            url.append('=');
            url.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        return url.toString();
    }

    @Override
    public String verifyAndExtractSteamId(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            throw new SteamOpenIdException("Missing OpenID callback parameters");
        }
        String mode = params.get("openid.mode");
        if (!"id_res".equals(mode)) {
            throw new SteamOpenIdException("Unexpected openid.mode: " + mode);
        }
        String claimedId = params.get("openid.claimed_id");
        if (claimedId == null) {
            throw new SteamOpenIdException("Missing openid.claimed_id");
        }

        // Re-post the exact same parameters with openid.mode=check_authentication so Steam can
        // verify its own signature.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);
        form.set("openid.mode", "check_authentication");

        String body;
        try {
            body = restClient.post()
                    .uri(providerUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Steam OpenID check_authentication request failed: {}", e.getMessage());
            throw new SteamOpenIdException("Steam OpenID verification failed", e);
        }

        if (body == null || !body.contains("is_valid:true")) {
            log.warn("Steam OpenID check_authentication returned invalid response: {}", body);
            throw new SteamOpenIdException("Steam OpenID response was not validated");
        }

        Matcher matcher = CLAIMED_ID_PATTERN.matcher(claimedId);
        if (!matcher.matches()) {
            throw new SteamOpenIdException("Malformed openid.claimed_id: " + claimedId);
        }
        return matcher.group(1);
    }
}
