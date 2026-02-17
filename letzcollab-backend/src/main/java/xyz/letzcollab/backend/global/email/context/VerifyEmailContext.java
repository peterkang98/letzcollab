package xyz.letzcollab.backend.global.email.context;

import java.util.Map;

public record VerifyEmailContext(String name, String token, String baseUrl) implements EmailContext{
	@Override
	public String getTemplateName() {
		return "mail/verify";
	}

	@Override
	public String getSubject() {
		return "이메일 인증을 완료 해주세요";
	}

	@Override
	public Map<String, Object> getVariables() {
		return Map.of(
				"name", name,
				"title", "이메일 인증",
				"content", "가입을 진심으로 축하드립니다.<br>\n 아래 버튼을 클릭하여 이메일 인증을 완료하고 서비스를 시작해 보세요.",
				"link", baseUrl + "/auth/verify-email?token=" + token
		);
	}
}
