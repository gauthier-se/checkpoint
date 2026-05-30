package com.seyzeriat.desktop.service;

import com.seyzeriat.desktop.exception.AuthenticationException;

public interface AuthenticationService {
    void login(String email, String password) throws AuthenticationException;
    void refreshTokens() throws AuthenticationException;
    void logout();
}
