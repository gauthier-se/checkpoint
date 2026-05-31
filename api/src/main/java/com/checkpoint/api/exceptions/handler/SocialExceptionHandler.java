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
import com.checkpoint.api.exceptions.CommentNotFoundException;
import com.checkpoint.api.exceptions.DuplicateReportException;
import com.checkpoint.api.exceptions.NotificationNotFoundException;
import com.checkpoint.api.exceptions.ReportNotFoundException;
import com.checkpoint.api.exceptions.ReviewAlreadyExistsException;
import com.checkpoint.api.exceptions.ReviewNotFoundException;
import com.checkpoint.api.exceptions.SelfFollowException;
import com.checkpoint.api.exceptions.UnauthorizedCommentAccessException;
import com.checkpoint.api.exceptions.UnauthorizedNotificationAccessException;

/**
 * Handles exceptions raised by social features: comments, reviews, reports,
 * follows, and notifications.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SocialExceptionHandler extends AbstractExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SocialExceptionHandler.class);

    /** Handles {@link CommentNotFoundException}. */
    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFound(CommentNotFoundException ex) {
        log.warn("Comment not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link UnauthorizedCommentAccessException} when a user modifies a comment they do not own. */
    @ExceptionHandler(UnauthorizedCommentAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedCommentAccess(UnauthorizedCommentAccessException ex) {
        log.warn("Unauthorized comment access: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /** Handles {@link ReportNotFoundException}. */
    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportNotFound(ReportNotFoundException ex) {
        log.warn("Report not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link DuplicateReportException} when a user reports a review they already reported. */
    @ExceptionHandler(DuplicateReportException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateReport(DuplicateReportException ex) {
        log.warn("Duplicate report: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Handles {@link ReviewNotFoundException}. */
    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReviewNotFound(ReviewNotFoundException ex) {
        log.warn("Review not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link ReviewAlreadyExistsException} when a play log already has a review. */
    @ExceptionHandler(ReviewAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleReviewAlreadyExists(ReviewAlreadyExistsException ex) {
        log.warn("Review already exists: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Handles {@link SelfFollowException} when a user tries to follow themselves. */
    @ExceptionHandler(SelfFollowException.class)
    public ResponseEntity<ErrorResponse> handleSelfFollow(SelfFollowException ex) {
        log.warn("Self-follow attempt: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Handles {@link NotificationNotFoundException}. */
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotificationNotFound(NotificationNotFoundException ex) {
        log.warn("Notification not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Handles {@link UnauthorizedNotificationAccessException} when a user accesses a notification they do not own. */
    @ExceptionHandler(UnauthorizedNotificationAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedNotificationAccess(UnauthorizedNotificationAccessException ex) {
        log.warn("Unauthorized notification access: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }
}
