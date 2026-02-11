package xyz.letzcollab.backend.global.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmailServiceTest {
	@Autowired
	private EmailService emailService;

	@Test
	@DisplayName("html 템플릿으로 이메일 전송 성공")
	void sendTemplateEmail_success() {
		TestContext context = new TestContext("테스트 메일 내용");

		assertDoesNotThrow(() -> emailService.sendTemplateEmail("user@example.com", context));
	}
}