package xyz.letzcollab.backend.global.event.dto;

import xyz.letzcollab.backend.global.email.context.EmailContext;

public record EmailEvent(
	String email,
	EmailContext context
) {
}
