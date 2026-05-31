package com.seyzeriat.desktop.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.seyzeriat.desktop.dto.UserDetailResult;
import com.seyzeriat.desktop.dto.UserResult;
import com.seyzeriat.desktop.exception.UnauthorizedException;
import com.seyzeriat.desktop.service.AuthenticationService;
import com.seyzeriat.desktop.service.UserService;

/**
 * API client implementation for {@link UserService}.
 * Handles HTTP communication with the backend to manage user resources.
 */
public class UserApiClient extends BaseApiClient implements UserService {

    /**
     * Constructs a new UserApiClient with the specified authentication service.
     *
     * @param authService the authentication service to use for securing requests
     */
    public UserApiClient(AuthenticationService authService) {
        super(authService);
    }

    /**
     * Constructs a new UserApiClient with the specified authentication service and HTTP client.
     *
     * @param authService the authentication service
     * @param httpClient  the HTTP client to use for requests
     */
    public UserApiClient(AuthenticationService authService, java.net.http.HttpClient httpClient) {
        super(authService, httpClient);
    }

    /**
     * Retrieves a list of all users.
     *
     * @return a list containing the user results
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public List<UserResult> getUsers() throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/users";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch users with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<UserResult>>() {});
    }

    /**
     * Retrieves detailed information about a specific user.
     *
     * @param id the unique identifier of the user
     * @return the detailed result for the specified user
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public UserDetailResult getUserDetail(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/users/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch user detail with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), UserDetailResult.class);
    }

    /**
     * Edits a specific user's information.
     *
     * @param id the unique identifier of the user to edit
     * @param body the JSON string representing the updated user information
     * @return the updated detailed user result
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public UserDetailResult editUser(String id, String body) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/users/" + id;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body));

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to edit user with status " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), UserDetailResult.class);
    }

    /**
     * Bans a specific user.
     *
     * @param id the unique identifier of the user to ban
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public void banUser(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/users/" + id + "/ban";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody());

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Failed to ban user with status " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Unbans a specific user.
     *
     * @param id the unique identifier of the user to unban
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized
     */
    @Override
    public void unbanUser(String id) throws IOException, InterruptedException, UnauthorizedException {
        String url = BASE_URL + "/admin/users/" + id + "/unban";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody());

        HttpResponse<String> response = sendWithAuth(builder);

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Failed to unban user with status " + response.statusCode() + ": " + response.body());
        }
    }
}
