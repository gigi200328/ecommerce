package com.ojt.ecommerce.backofficeinventory.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ojt.ecommerce.backofficeinventory.dto.ErrorResponse;

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

	// 3. Logic အမှားများ (ဥပမာ- မိမိကိုယ်ကို Parent အဖြစ် set လုပ်ခြင်း) (HTTP 400
	// Bad Request)
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
		ErrorResponse errorResponse = ErrorResponse.builder().status(HttpStatus.BAD_REQUEST.value())
				.error(HttpStatus.BAD_REQUEST.getReasonPhrase()).message(ex.getMessage()).timestamp(LocalDateTime.now())
				.build();

		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	// 4. မမျှော်လင့်ထားသော အခြား Server Error များအတွက် Catch-all (HTTP 500
	// Internal Server Error)
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
		ErrorResponse errorResponse = ErrorResponse.builder().status(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
				.message("An unexpected error occurred: " + ex.getMessage()).timestamp(LocalDateTime.now()).build();

		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
