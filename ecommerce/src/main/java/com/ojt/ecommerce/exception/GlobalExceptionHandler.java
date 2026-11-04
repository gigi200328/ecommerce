package com.ojt.ecommerce.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ojt.ecommerce.backofficeinventory.dto.ErrorResponse;
import com.ojt.ecommerce.storefront.exception.AccessDeniedException;
import com.ojt.ecommerce.storefront.exception.ConflictException;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- 0. Common Error Response Builder ---
    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponseWithValidation(HttpStatus status, String message, Map<String, String> errors) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .validationErrors(errors)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }


    // --- 1. Entity Not Found - 404 ---
    @ExceptionHandler({ EntityNotFoundException.class, ResourceNotFoundException.class })
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // --- 2. Validation Error - 400 ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return buildErrorResponseWithValidation(
                HttpStatus.BAD_REQUEST, 
                "Validation failed for request parameters", 
                errors
        );
    }

    // --- 3. Illegal Argument & Invalid Request - 400 ---
    @ExceptionHandler({ IllegalArgumentException.class, InvalidRequestException.class })
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(Exception ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // --- 4. Business Runtime Exceptions - 400 ---
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // --- 5. Access Denied - 403 ---
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // --- 6. Conflict - 409 ---
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // --- 7. Database Constraint Violation - 409 ---
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String rootCauseMessage = ex.getMostSpecificCause().getMessage();
        if (rootCauseMessage == null) {
            rootCauseMessage = "";
        }
        rootCauseMessage = rootCauseMessage.toLowerCase();

        String message = "This record cannot be deleted or modified because it is currently in use elsewhere.";

        if (rootCauseMessage.contains("tag") || rootCauseMessage.contains("product_tags")) {
            message = "This tag cannot be deleted because it is still being used by products.";
        } else if (rootCauseMessage.contains("brand") || rootCauseMessage.contains("product_brands")) {
            message = "This brand cannot be deleted because it is still being used by products. Please set its status to Inactive instead.";
        } else if (rootCauseMessage.contains("category")) {
            message = "This category cannot be deleted because it is still being used in the system.";
        }

        return buildErrorResponse(HttpStatus.CONFLICT, message);
    }

    // --- 8. Generic Exception (Catch-all) - 500 ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + ex.getMessage());
    }
}