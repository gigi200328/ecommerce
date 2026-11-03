
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

	// 1. Entity မတွေ့သည့်အခါ (HTTP 404 Not Found)
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
		ErrorResponse errorResponse = ErrorResponse.builder().status(HttpStatus.NOT_FOUND.value())
				.error(HttpStatus.NOT_FOUND.getReasonPhrase()).message(ex.getMessage()).timestamp(LocalDateTime.now())
				.build();

		return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
	}

	// 2. DTO Validation (@Valid) မအောင်သည့်အခါ (HTTP 400 Bad Request)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();

		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});

		ErrorResponse errorResponse = ErrorResponse.builder().status(HttpStatus.BAD_REQUEST.value())
				.error(HttpStatus.BAD_REQUEST.getReasonPhrase()).message("Validation failed for request parameters")
				.validationErrors(errors).timestamp(LocalDateTime.now()).build();

		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	// 3. Logic အမှားများ (HTTP 400 Bad Request)
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
		ErrorResponse errorResponse = ErrorResponse.builder().status(HttpStatus.BAD_REQUEST.value())
				.error(HttpStatus.BAD_REQUEST.getReasonPhrase()).message(ex.getMessage()).timestamp(LocalDateTime.now())
				.build();

		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	// 3.1 Business Runtime Exceptions (ဥပမာ- Last Admin ကာကွယ်ခြင်း၊ မိမိကိုယ်ကို ဖျက်ခြင်း တားဆီးခြင်း)
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
		ErrorResponse errorResponse = ErrorResponse.builder().status(HttpStatus.BAD_REQUEST.value())
				.error(HttpStatus.BAD_REQUEST.getReasonPhrase()).message(ex.getMessage()).timestamp(LocalDateTime.now())
				.build();

		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	// 4. မမျှော်လင့်ထားသော အခြား Server Error များအတွက် Catch-all (HTTP 500 Internal Server Error)
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
		ErrorResponse errorResponse = ErrorResponse.builder().status(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
				.message("An unexpected error occurred: " + ex.getMessage()).timestamp(LocalDateTime.now()).build();

		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
		return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(InvalidRequestException.class)
	public ResponseEntity<Map<String, String>> handleInvalidRequest(InvalidRequestException ex) {
		return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
		return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<Map<String, String>> handleConflict(ConflictException ex) {
		return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
	}

	private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String message) {
		Map<String, String> response = new HashMap<>();
		response.put("message", message);
		return new ResponseEntity<>(response, status);
	}
    // Common Error Response Builder
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status, String message) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }

    // 1. Entity Not Found - 404
    @ExceptionHandler({
            EntityNotFoundException.class,
            ResourceNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 2. Validation Error - 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();

            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed for request parameters")
                .validationErrors(errors)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(
                errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 3. Illegal Argument - 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 4. Invalid Request - 400
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            InvalidRequestException ex) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 5. Access Denied - 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex) {

        return buildErrorResponse(
                HttpStatus.FORBIDDEN, ex.getMessage());
    }

 // 6. Conflict - 409
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex) {

        String originalMessage = ex.getMessage();
        String message = originalMessage;

        
        if (originalMessage != null && (originalMessage.toLowerCase().contains("brand already exists") || originalMessage.toLowerCase().contains("exists"))) {
            message = "This brand name already exists. Please choose a different brand name and try again.";
        }

        return buildErrorResponse(
                HttpStatus.CONFLICT, message);
    }

    // 7. Database Constraint Violation - 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex) {

        String rootCauseMessage = ex.getMostSpecificCause()
                .getMessage();

        if (rootCauseMessage == null) {
            rootCauseMessage = "";
        }

        rootCauseMessage = rootCauseMessage.toLowerCase();

        String message =
                "This record cannot be deleted or modified because it is currently in use elsewhere.";

        if (rootCauseMessage.contains("tag")
                || rootCauseMessage.contains("product_tags")) {

            message = "This tag cannot be deleted because it is still being used by products.";

        } else if (rootCauseMessage.contains("brand")
                || rootCauseMessage.contains("product_brands")) {

            message = "This brand cannot be deleted because it is still being used by products. Please set its status to Inactive instead.";

        } else if (rootCauseMessage.contains("category")) {

            message = "This category cannot be deleted because it is still being used in the system.";
        }

        return buildErrorResponse(
                HttpStatus.CONFLICT, message);
    }

    // 8. Generic Exception - 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex) {

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.");
    }
}
