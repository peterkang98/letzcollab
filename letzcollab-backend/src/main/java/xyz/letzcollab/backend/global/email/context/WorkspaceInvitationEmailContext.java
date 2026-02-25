package xyz.letzcollab.backend.global.email.context;

import java.util.Map;

public record WorkspaceInvitationEmailContext(
	String inviterName,
	String workspaceName,
	String token,
	String baseUrl
) implements EmailContext{
	@Override
	public String getTemplateName() {
		return "mail/workspace-invitation";
	}

	@Override
	public String getSubject() {
		return String.format("[Letz Collab] %s님이 '%s' 워크스페이스로 초대하셨습니다.", inviterName, workspaceName);
	}

	@Override
	public Map<String, Object> getVariables() {
		return Map.of(
				"title", "워크스페이스 초대",
				"inviterName", inviterName,
				"workspaceName", workspaceName,
				"link", baseUrl + "/workspaces/verify-invitation-email?token=" + token
		);
	}
}
