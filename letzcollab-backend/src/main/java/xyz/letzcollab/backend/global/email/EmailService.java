package xyz.letzcollab.backend.global.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import xyz.letzcollab.backend.global.email.context.EmailContext;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;

	private static final String fromEmail = "no-reply@mail.letzcollab.xyz";
	private static final String fromPersonalName = "LetzCollab";

	public void sendTemplateEmail(String to, EmailContext emailContext) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			Context context = new Context();
			context.setVariables(emailContext.getVariables());

			String htmlContent = templateEngine.process(emailContext.getTemplateName(), context);

			helper.setFrom(fromEmail, fromPersonalName);
			helper.setTo(to);
			helper.setSubject(emailContext.getSubject());
			helper.setText(htmlContent, true);

			mailSender.send(message);
		} catch (MessagingException | UnsupportedEncodingException e) {
			log.error("메일 발송 실패 - 수신자: {}, 제목: {}", to, emailContext.getSubject());
			throw new RuntimeException("메일 발송 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}
}
