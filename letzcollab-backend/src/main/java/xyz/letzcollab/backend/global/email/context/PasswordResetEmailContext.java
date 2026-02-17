package xyz.letzcollab.backend.global.email.context;

import java.util.Map;

public record PasswordResetEmailContext(String name, String token, String baseUrl) implements EmailContext {
	@Override
	public String getTemplateName() {
		return "mail/verify";
	}

	@Override
	public String getSubject() {
		return "비밀번호를 재설정 해주세요";
	}

	@Override
	public Map<String, Object> getVariables() {
		return Map.of(
				"name", name,
				"title", "비밀번호 재설정",
				"content", "아래 버튼을 클릭하여 비밀번호를 재설정하세요.<br>\n본인이 요청하지 않았다면 이 메일을 무시해 주세요.",
				"link", baseUrl + "/auth/password/reset?token=" + token
		);
	}
}
