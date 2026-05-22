package com.checkpoint.api.controllers;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.checkpoint.api.exceptions.CommentNotFoundException;
import com.checkpoint.api.exceptions.InvalidFavoritesException;
import com.checkpoint.api.exceptions.InvalidFileException;
import com.checkpoint.api.exceptions.GameAlreadyInListException;
import com.checkpoint.api.exceptions.GameListNotFoundException;
import com.checkpoint.api.exceptions.GameNotInListException;
import com.checkpoint.api.exceptions.DuplicateReportException;
import com.checkpoint.api.exceptions.ExternalApiUnavailableException;
import com.checkpoint.api.exceptions.ProfilePrivateException;
import com.checkpoint.api.exceptions.IgdbApiException;
import com.checkpoint.api.exceptions.ExternalGameNotFoundException;
import com.checkpoint.api.exceptions.GameAlreadyInLibraryException;
import com.checkpoint.api.exceptions.GameNotFoundException;
import com.checkpoint.api.exceptions.GameNotInLibraryException;
import com.checkpoint.api.exceptions.GameReferencedException;
import com.checkpoint.api.exceptions.GameAlreadyInBacklogException;
import com.checkpoint.api.exceptions.GameAlreadyInWishlistException;
import com.checkpoint.api.exceptions.GameNotInBacklogException;
import com.checkpoint.api.exceptions.GameNotInWishlistException;
import com.checkpoint.api.exceptions.InvalidRefreshTokenException;
import com.checkpoint.api.exceptions.InvalidTokenException;
import com.checkpoint.api.exceptions.InvalidTotpCodeException;
import com.checkpoint.api.exceptions.RegistrationConflictException;
import com.checkpoint.api.exceptions.ReportNotFoundException;
import com.checkpoint.api.exceptions.PlayLogNotFoundException;
import com.checkpoint.api.exceptions.PseudoAlreadyExistsException;
import com.checkpoint.api.exceptions.RateNotFoundException;
import com.checkpoint.api.exceptions.ReviewAlreadyExistsException;
import com.checkpoint.api.exceptions.ReviewNotFoundException;
import com.checkpoint.api.exceptions.InvalidSteamIdException;
import com.checkpoint.api.exceptions.SelfFollowException;
import com.checkpoint.api.exceptions.SteamAccountNotLinkedException;
import com.checkpoint.api.exceptions.SteamApiException;
import com.checkpoint.api.exceptions.SteamLibraryPrivateException;
import com.checkpoint.api.exceptions.SteamOpenIdException;
import com.checkpoint.api.exceptions.TagNotFoundException;
import com.checkpoint.api.exceptions.DuplicateTagException;
import com.checkpoint.api.exceptions.NewsNotFoundException;
import com.checkpoint.api.exceptions.NotificationNotFoundException;
import com.checkpoint.api.exceptions.UnauthorizedCommentAccessException;
import com.checkpoint.api.exceptions.UnauthorizedListAccessException;
import com.checkpoint.api.exceptions.UnauthorizedNotificationAccessException;
import com.checkpoint.api.exceptions.UserBannedException;
import com.checkpoint.api.exceptions.UserNotFoundException;

/**
 * Global exception handler for REST controllers.
 * Provides consistent error responses across all endpoints.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles GameListNotFoundException when a game list is not found.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(GameListNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGameListNotFound(GameListNotFoundException ex) {
        log.warn("Game list not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles CommentNotFoundException when a comment is not found.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFound(CommentNotFoundException ex) {
        log.warn("Comment not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles UnauthorizedCommentAccessException when a user tries to modify or delete a comment they do not own.
     *
     * @param ex the exception
     * @return error response with 403 status
     */
    @ExceptionHandler(UnauthorizedCommentAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedCommentAccess(UnauthorizedCommentAccessException ex) {
        log.warn("Unauthorized comment access: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles GameAlreadyInListException when a game is already present in a list.
     *
     * @param ex the exception
     * @return error response with 409 status
     */
    @ExceptionHandler(GameAlreadyInListException.class)
    public ResponseEntity<ErrorResponse> handleGameAlreadyInList(GameAlreadyInListException ex) {
        log.warn("Game already in list: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles GameNotInListException when a game is not found in a list.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(GameNotInListException.class)
    public ResponseEntity<ErrorResponse> handleGameNotInList(GameNotInListException ex) {
        log.warn("Game not in list: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles UnauthorizedListAccessException when a user tries to access or modify a list they do not own.
     *
     * @param ex the exception
     * @return error response with 403 status
     */
    @ExceptionHandler(UnauthorizedListAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedListAccess(UnauthorizedListAccessException ex) {
        log.warn("Unauthorized list access: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles ReportNotFoundException when a report is not found.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportNotFound(ReportNotFoundException ex) {
        log.warn("Report not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles DuplicateReportException when a user tries to report a review they already reported.
     *
     * @param ex the exception
     * @return error response with 409 status
     */
    @ExceptionHandler(DuplicateReportException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateReport(DuplicateReportException ex) {
        log.warn("Duplicate report: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles GameReferencedException when an admin attempts to delete a game
     * that is still referenced by user-owned data or DLC entries.
     *
     * @param ex the exception
     * @return error response with 409 status and per-category blocking counts
     */
    @ExceptionHandler(GameReferencedException.class)
    public ResponseEntity<GameReferencedResponse> handleGameReferenced(GameReferencedException ex) {
        log.warn("Game referenced — refusing delete: {}", ex.getMessage());

        GameReferencedResponse error = new GameReferencedResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                "Game cannot be deleted because it is referenced by existing data",
                LocalDateTime.now(),
                ex.getBlockingReferences()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles GameNotFoundException.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGameNotFound(GameNotFoundException ex) {
        log.warn("Game not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles ExternalGameNotFoundException when a game is not found in IGDB.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(ExternalGameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleExternalGameNotFound(ExternalGameNotFoundException ex) {
        log.warn("External game not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles ExternalApiUnavailableException when IGDB API is down or rate limited.
     *
     * @param ex the exception
     * @return error response with 503 status
     */
    @ExceptionHandler(ExternalApiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleExternalApiUnavailable(ExternalApiUnavailableException ex) {
        log.error("External API unavailable: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handles IgdbApiException as a safety net when the exception is not caught
     * and re-wrapped by a service layer.
     *
     * @param ex the exception
     * @return error response with 503 status
     */
    @ExceptionHandler(IgdbApiException.class)
    public ResponseEntity<ErrorResponse> handleIgdbApiException(IgdbApiException ex) {
        log.error("IGDB API error: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handles InvalidSteamIdException when a SteamID is malformed or unknown to Steam.
     *
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(InvalidSteamIdException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSteamId(InvalidSteamIdException ex) {
        log.warn("Invalid Steam ID: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles SteamApiException when the Steam Web API call fails.
     *
     * @param ex the exception
     * @return error response with 503 status
     */
    @ExceptionHandler(SteamApiException.class)
    public ResponseEntity<ErrorResponse> handleSteamApiException(SteamApiException ex) {
        log.error("Steam API error: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handles SteamAccountNotLinkedException when an action requires a linked Steam account
     * but the user has none.
     *
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(SteamAccountNotLinkedException.class)
    public ResponseEntity<ErrorResponse> handleSteamAccountNotLinked(SteamAccountNotLinkedException ex) {
        log.warn("Steam account not linked: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles SteamLibraryPrivateException when the user's Steam library visibility
     * prevents reading owned games.
     *
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(SteamLibraryPrivateException.class)
    public ResponseEntity<ErrorResponse> handleSteamLibraryPrivate(SteamLibraryPrivateException ex) {
        log.warn("Steam library private: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles SteamOpenIdException when the Steam OpenID verification fails.
     *
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(SteamOpenIdException.class)
    public ResponseEntity<ErrorResponse> handleSteamOpenIdException(SteamOpenIdException ex) {
        log.warn("Steam OpenID error: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles GameAlreadyInLibraryException when a game is already in the user's library.
     *
     * @param ex the exception
     * @return error response with 409 status
     */
    @ExceptionHandler(GameAlreadyInLibraryException.class)
    public ResponseEntity<ErrorResponse> handleGameAlreadyInLibrary(GameAlreadyInLibraryException ex) {
        log.warn("Game already in library: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles GameNotInLibraryException when a game is not found in the user's library.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(GameNotInLibraryException.class)
    public ResponseEntity<ErrorResponse> handleGameNotInLibrary(GameNotInLibraryException ex) {
        log.warn("Game not in library: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles GameAlreadyInWishlistException when a game is already in the user's wishlist.
     *
     * @param ex the exception
     * @return error response with 409 status
     */
    @ExceptionHandler(GameAlreadyInWishlistException.class)
    public ResponseEntity<ErrorResponse> handleGameAlreadyInWishlist(GameAlreadyInWishlistException ex) {
        log.warn("Game already in wishlist: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles GameNotInWishlistException when a game is not found in the user's wishlist.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(GameNotInWishlistException.class)
    public ResponseEntity<ErrorResponse> handleGameNotInWishlist(GameNotInWishlistException ex) {
        log.warn("Game not in wishlist: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles GameAlreadyInBacklogException when a game is already in the user's backlog.
     *
     * @param ex the exception
     * @return error response with 409 status
     */
    @ExceptionHandler(GameAlreadyInBacklogException.class)
    public ResponseEntity<ErrorResponse> handleGameAlreadyInBacklog(GameAlreadyInBacklogException ex) {
        log.warn("Game already in backlog: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles GameNotInBacklogException when a game is not found in the user's backlog.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(GameNotInBacklogException.class)
    public ResponseEntity<ErrorResponse> handleGameNotInBacklog(GameNotInBacklogException ex) {
        log.warn("Game not in backlog: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles PlayLogNotFoundException when a play log is not found.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(PlayLogNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePlayLogNotFound(PlayLogNotFoundException ex) {
        log.warn("Play log not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles ReviewNotFoundException when a review is not found for a play log.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReviewNotFound(ReviewNotFoundException ex) {
        log.warn("Review not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles ReviewAlreadyExistsException when a play log already has a review.
     *
     * @param ex the exception
     * @return error response with 409 status
     */
    @ExceptionHandler(ReviewAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleReviewAlreadyExists(ReviewAlreadyExistsException ex) {
        log.warn("Review already exists: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles RateNotFoundException when a rating is not found for a user and game.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(RateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRateNotFound(RateNotFoundException ex) {
        log.warn("Rating not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles UserNotFoundException when a user is not found.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles UserBannedException when a banned user attempts to authenticate.
     *
     * @param ex the exception
     * @return error response with 403 status
     */
    @ExceptionHandler(UserBannedException.class)
    public ResponseEntity<ErrorResponse> handleUserBanned(UserBannedException ex) {
        log.warn("Banned user attempted access: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles ProfilePrivateException when a non-owner tries to access private profile data.
     *
     * @param ex the exception
     * @return error response with 403 status
     */
    @ExceptionHandler(ProfilePrivateException.class)
    public ResponseEntity<ErrorResponse> handleProfilePrivate(ProfilePrivateException ex) {
        log.warn("Private profile access: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles SelfFollowException when a user tries to follow themselves.
     *
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(SelfFollowException.class)
    public ResponseEntity<ErrorResponse> handleSelfFollow(SelfFollowException ex) {
        log.warn("Self-follow attempt: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles TagNotFoundException when a tag is not found.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTagNotFound(TagNotFoundException ex) {
        log.warn("Tag not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles DuplicateTagException when a user tries to create a tag with a duplicate name.
     *
     * @param ex the exception
     * @return error response with 409 status
     */
    @ExceptionHandler(DuplicateTagException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTag(DuplicateTagException ex) {
        log.warn("Duplicate tag: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles NewsNotFoundException when a news article is not found.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(NewsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNewsNotFound(NewsNotFoundException ex) {
        log.warn("News not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles NotificationNotFoundException when a notification is not found.
     *
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotificationNotFound(NotificationNotFoundException ex) {
        log.warn("Notification not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles UnauthorizedNotificationAccessException when a user tries to access a notification they do not own.
     *
     * @param ex the exception
     * @return error response with 403 status
     */
    @ExceptionHandler(UnauthorizedNotificationAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedNotificationAccess(UnauthorizedNotificationAccessException ex) {
        log.warn("Unauthorized notification access: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles missing required request parameters.
     *
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        log.warn("Missing parameter: {}", message);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles validation errors from @Valid annotated request bodies.
     *
     * @param ex the exception
     * @return error response with 400 status containing the first validation error
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        log.warn("Validation error: {}", message);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles IllegalArgumentException for bad requests.
     *
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles RegistrationConflictException when a registration attempt uses a
     * duplicate email or pseudo.
     *
     * @param ex the exception
     * @return error response with 409 status
     */
    @ExceptionHandler(RegistrationConflictException.class)
    public ResponseEntity<ErrorResponse> handleRegistrationConflict(RegistrationConflictException ex) {
        log.warn("Registration conflict: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles InvalidRefreshTokenException when a refresh token is missing, revoked, or expired.
     * Returns 401 so clients can redirect to login.
     *
     * @param ex the exception
     * @return error response with 401 status
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        log.warn("Invalid refresh token: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles InvalidTokenException when a password reset token is invalid or expired.
     *
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(InvalidTokenException ex) {
        log.warn("Invalid token: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles access denied (insufficient privileges).
     *
     * @param ex the exception
     * @return error response with 403 status
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "You do not have permission to access this resource",
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles type mismatches for path variables and request parameters (e.g., String to UUID).
     *
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Type mismatch for parameter '%s'. Failed to convert value to required type.", ex.getName());
        log.warn("Type mismatch error: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles PseudoAlreadyExistsException when a user tries to update their pseudo
     * to one that is already taken.
     *
     * @param ex the exception
     * @return error response with 409 status
     */
    @ExceptionHandler(PseudoAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlePseudoAlreadyExists(PseudoAlreadyExistsException ex) {
        log.warn("Pseudo already exists: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles InvalidFavoritesException when a favorites update request is rejected
     * (duplicate gameIds, unknown game, etc.).
     *
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(InvalidFavoritesException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFavorites(InvalidFavoritesException ex) {
        log.warn("Invalid favorites: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles InvalidFileException when an uploaded file fails validation.
     *
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException ex) {
        log.warn("Invalid file upload: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles InvalidTotpCodeException when a submitted TOTP code is invalid.
     *
     * @param ex the exception
     * @return error response with 401 status
     */
    @ExceptionHandler(InvalidTotpCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTotpCode(InvalidTotpCodeException ex) {
        log.warn("Invalid TOTP code: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles all other exceptions.
     *
     * @param ex the exception
     * @return error response with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Standard error response format.
     */
    public record ErrorResponse(
            int status,
            String error,
            String message,
            LocalDateTime timestamp
    ) {}

    /**
     * Error response used for {@link GameReferencedException}, including the
     * per-category counts of references blocking the deletion.
     */
    public record GameReferencedResponse(
            int status,
            String error,
            String message,
            LocalDateTime timestamp,
            Map<String, Long> blockingReferences
    ) {}
}
