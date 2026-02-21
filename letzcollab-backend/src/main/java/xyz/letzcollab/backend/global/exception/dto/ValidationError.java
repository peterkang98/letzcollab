package xyz.letzcollab.backend.global.exception.dto;

import org.springframework.validation.FieldError;

public record ValidationError(String field, String message) {
	public static ValidationError of(FieldError fieldError) {
		return new ValidationError(fieldError.getField(), fieldError.getDefaultMessage());
	}
}
