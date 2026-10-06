package com.tellpal.v2.user.web.mobile;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Keeps the controlled Library errors in the mobile ProblemDetail contract. */
@RestControllerAdvice(assignableTypes = LibraryHomeTestController.class)
@Profile({"local", "test"})
public class LibraryHomeTestExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handleRejectedRequest(ResponseStatusException exception, HttpServletRequest request) {
        ProblemDetail problem = exception.getBody();
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("errorCode", switch (exception.getStatusCode().value()) {
            case 401 -> "firebase_auth_error";
            case 403 -> "library_test_access_denied";
            case 400 -> "library_language_invalid";
            default -> "library_request_rejected";
        });
        return problem;
    }
}
