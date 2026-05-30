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

public class UserApiClient extends BaseApiClient implements UserService {

    public UserApiClient(AuthenticationService authService) {
        super(authService);
    }

    public UserApiClient(AuthenticationService authService, java.net.http.HttpClient httpClient) {
        super(authService, httpClient);
    }

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
