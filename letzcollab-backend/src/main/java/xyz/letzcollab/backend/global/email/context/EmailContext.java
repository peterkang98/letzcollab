package xyz.letzcollab.backend.global.email.context;

import java.util.Map;

public interface EmailContext {
	String getTemplateName();
	String getSubject();
	Map<String, Object> getVariables();
}
