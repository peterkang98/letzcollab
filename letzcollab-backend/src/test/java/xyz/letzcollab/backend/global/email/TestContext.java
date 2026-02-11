package xyz.letzcollab.backend.global.email;


import xyz.letzcollab.backend.global.email.context.EmailContext;

import java.util.Map;

public record TestContext(String content) implements EmailContext {

	@Override
	public String getTemplateName() {
		return "test-mail";
	}

	@Override
	public String getSubject() {
		return "테스트 메일 제목";
	}

	@Override
	public Map<String, Object> getVariables() {
		return Map.of("content", content);
	}
}
