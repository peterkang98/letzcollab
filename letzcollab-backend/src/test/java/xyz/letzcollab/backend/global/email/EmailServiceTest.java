package xyz.letzcollab.backend.global.email;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
	@Mock
	private JavaMailSender mailSender;

	@Mock
	private SpringTemplateEngine templateEngine;

	@InjectMocks
	private EmailService emailService;

	@Test
	@DisplayName("html 템플릿으로 이메일 전송 성공")
	void sendTemplateEmail_success() {
		// MimeMessage 생성 Mocking
		MimeMessage mimeMessage = Mockito.mock(MimeMessage.class);
		given(mailSender.createMimeMessage()).willReturn(mimeMessage);

		// 템플릿 처리 결과 Mocking
		given(templateEngine.process(anyString(), any(Context.class)))
				.willReturn("<html>테스트 메일 내용</html>");

		TestContext context = new TestContext("테스트 메일 내용");

		assertDoesNotThrow(() -> emailService.sendTemplateEmail("peterkang98@gmail.com", context));

		verify(mailSender, times(1)).send(any(MimeMessage.class));
	}
}