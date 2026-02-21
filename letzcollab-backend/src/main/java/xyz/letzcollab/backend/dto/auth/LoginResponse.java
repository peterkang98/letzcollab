package xyz.letzcollab.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
	String accessToken,
	String name,
	String email
) {
	public LoginResponse withoutToken() {
		return new LoginResponse(null, this.name, this.email);
	}
}