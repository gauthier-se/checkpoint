package com.seyzeriat.desktop.service;

import java.io.IOException;
import java.util.List;
import com.seyzeriat.desktop.dto.UserResult;
import com.seyzeriat.desktop.dto.UserDetailResult;
import com.seyzeriat.desktop.exception.UnauthorizedException;

/**
 * Service interface for managing users.
 * Provides operations to retrieve, edit, and manage user accounts.
 */
public interface UserService {

    /**
     * Retrieves a list of all users.
     *
     * @return a list containing the user results
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    List<UserResult> getUsers() throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Retrieves detailed information about a specific user.
     *
     * @param id the unique identifier of the user
     * @return the detailed result for the specified user
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    UserDetailResult getUserDetail(String id) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Edits a specific user's information.
     *
     * @param id the unique identifier of the user to edit
     * @param body the JSON string representing the updated user information
     * @return the updated detailed user result
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    UserDetailResult editUser(String id, String body) throws IOException, InterruptedException, UnauthorizedException;
    /**
     * Bans a specific user.
     *
     * @param id the unique identifier of the user to ban
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    void banUser(String id) throws IOException, InterruptedException, UnauthorizedException;

    /**
     * Unbans a specific user.
     *
     * @param id the unique identifier of the user to unban
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws UnauthorizedException if the user is not authorized to access this resource
     */
    void unbanUser(String id) throws IOException, InterruptedException, UnauthorizedException;
}
