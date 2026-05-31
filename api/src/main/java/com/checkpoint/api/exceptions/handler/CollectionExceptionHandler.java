package com.checkpoint.api.exceptions.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.checkpoint.api.dto.error.ErrorResponse;
import com.checkpoint.api.exceptions.DuplicateTagException;
import com.checkpoint.api.exceptions.GameAlreadyInBacklogException;
import com.checkpoint.api.exceptions.GameAlreadyInLibraryException;
import com.checkpoint.api.exceptions.GameAlreadyInListException;
import com.checkpoint.api.exceptions.GameAlreadyInWishlistException;
import com.checkpoint.api.exceptions.GameListNotFoundException;
import com.checkpoint.api.exceptions.GameNotInBacklogException;
import com.checkpoint.api.exceptions.GameNotInLibraryException;
import com.checkpoint.api.exceptions.GameNotInListException;
import com.checkpoint.api.exceptions.GameNotInWishlistException;
import com.checkpoint.api.exceptions.InvalidFavoritesException;
import com.checkpoint.api.exceptions.PlayLogNotFoundException;
import com.checkpoint.api.exceptions.RateNotFoundException;
import com.checkpoint.api.exceptions.TagNotFoundException;
import com.checkpoint.api.exceptions.UnauthorizedListAccessException;

/**
 * Handles exceptions raised by a user's personal collection: library, wishlist,
 * backlog, favorites, lists, tags, play logs, and ratings.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class CollectionExceptionHandler extends AbstractExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CollectionExceptionHandler.class);

    /** Handles {@link GameAlreadyInLibraryException}. */
    @ExceptionHandler(GameAlreadyInLibraryException.class)
    public ResponseEntity<ErrorResponse> handleGameAlreadyInLibrary(GameAlreadyInLibraryException ex) {
        log.warn("Game already in library: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Handles {@link GameNotInLibraryException}. */
    @ExceptionHandler(GameNotInLibraryException.class)
    public ResponseEntity<ErrorResponse> handleGameNotInLibrary(GameNotInLibraryException ex) {
        log.warn("Game not in library: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link GameAlreadyInWishlistException}. */
    @ExceptionHandler(GameAlreadyInWishlistException.class)
    public ResponseEntity<ErrorResponse> handleGameAlreadyInWishlist(GameAlreadyInWishlistException ex) {
        log.warn("Game already in wishlist: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Handles {@link GameNotInWishlistException}. */
    @ExceptionHandler(GameNotInWishlistException.class)
    public ResponseEntity<ErrorResponse> handleGameNotInWishlist(GameNotInWishlistException ex) {
        log.warn("Game not in wishlist: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link GameAlreadyInBacklogException}. */
    @ExceptionHandler(GameAlreadyInBacklogException.class)
    public ResponseEntity<ErrorResponse> handleGameAlreadyInBacklog(GameAlreadyInBacklogException ex) {
        log.warn("Game already in backlog: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Handles {@link GameNotInBacklogException}. */
    @ExceptionHandler(GameNotInBacklogException.class)
    public ResponseEntity<ErrorResponse> handleGameNotInBacklog(GameNotInBacklogException ex) {
        log.warn("Game not in backlog: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link InvalidFavoritesException} when a favorites update is rejected. */
    @ExceptionHandler(InvalidFavoritesException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFavorites(InvalidFavoritesException ex) {
        log.warn("Invalid favorites: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Handles {@link GameListNotFoundException}. */
    @ExceptionHandler(GameListNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGameListNotFound(GameListNotFoundException ex) {
        log.warn("Game list not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link GameAlreadyInListException}. */
    @ExceptionHandler(GameAlreadyInListException.class)
    public ResponseEntity<ErrorResponse> handleGameAlreadyInList(GameAlreadyInListException ex) {
        log.warn("Game already in list: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Handles {@link GameNotInListException}. */
    @ExceptionHandler(GameNotInListException.class)
    public ResponseEntity<ErrorResponse> handleGameNotInList(GameNotInListException ex) {
        log.warn("Game not in list: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link UnauthorizedListAccessException} when a user accesses a list they do not own. */
    @ExceptionHandler(UnauthorizedListAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedListAccess(UnauthorizedListAccessException ex) {
        log.warn("Unauthorized list access: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /** Handles {@link TagNotFoundException}. */
    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTagNotFound(TagNotFoundException ex) {
        log.warn("Tag not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link DuplicateTagException} when a user creates a tag with a duplicate name. */
    @ExceptionHandler(DuplicateTagException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTag(DuplicateTagException ex) {
        log.warn("Duplicate tag: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Handles {@link PlayLogNotFoundException}. */
    @ExceptionHandler(PlayLogNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePlayLogNotFound(PlayLogNotFoundException ex) {
        log.warn("Play log not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link RateNotFoundException}. */
    @ExceptionHandler(RateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRateNotFound(RateNotFoundException ex) {
        log.warn("Rating not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
