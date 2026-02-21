package xyz.letzcollab.backend.global.persistence;

import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import xyz.letzcollab.backend.global.security.userdetails.CustomUserDetails;

import java.util.Optional;

@Component
public class CustomAuditorAware implements AuditorAware<String> {
	@Override
	@NonNull
	public Optional<String> getCurrentAuditor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		return Optional.of(userDetails.getPublicId());
	}
}
