package com.seyzeriat.desktop.service;

import java.io.IOException;
import java.util.List;
import com.seyzeriat.desktop.dto.UserResult;
import com.seyzeriat.desktop.dto.UserDetailResult;
import com.seyzeriat.desktop.exception.UnauthorizedException;

public interface UserService {
    List<UserResult> getUsers() throws IOException, InterruptedException, UnauthorizedException;
    UserDetailResult getUserDetail(String id) throws IOException, InterruptedException, UnauthorizedException;
    UserDetailResult editUser(String id, String body) throws IOException, InterruptedException, UnauthorizedException;
    void banUser(String id) throws IOException, InterruptedException, UnauthorizedException;
    void unbanUser(String id) throws IOException, InterruptedException, UnauthorizedException;
}
