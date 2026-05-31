package com.seyzeriat.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing the token pair login response from the API.
 * The API returns {@code accessToken} (JWT, 24h) and {@code refreshToken} (opaque, 7d).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponseDto {

    private String accessToken;
    private String refreshToken;

    /**
     * Default constructor for LoginResponseDto.
     */
    public LoginResponseDto() {}

    /**
     * Gets the access token.
     *
     * @return the access token
     */
    public String getAccessToken() { return accessToken; }

    /**
     * Sets the access token.
     *
     * @param accessToken the access token to set
     */
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    /**
     * Gets the refresh token.
     *
     * @return the refresh token
     */
    public String getRefreshToken() { return refreshToken; }

    /**
     * Sets the refresh token.
     *
     * @param refreshToken the refresh token to set
     */
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
