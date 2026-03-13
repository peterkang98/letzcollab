package xyz.letzcollab.backend.global.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import xyz.letzcollab.backend.global.email.EmailService;
import xyz.letzcollab.backend.global.event.dto.EmailEvent;
import xyz.letzcollab.backend.global.exception.CustomException;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventListener {
	private final EmailService emailService;

	@Async("emailExecutor")
	@Retryable(
		retryFor = {CustomException.class},
		maxAttempts = 3,
		backoff = @Backoff(delay = 2000, multiplier = 2.0),
		recover = "recover"
	)
	@TransactionalEventListener
	public void handleEmailEvent(EmailEvent event) {
		log.info("이메일 발송 시작 - 수신자: {}", event.email());
		emailService.sendTemplateEmail(event.email(), event.context());
	}

	@Recover
	public void recover(CustomException e, EmailEvent event) {
		log.error("이메일 발송 3회 재시도 모두 실패 - 수신자: {}, 사유: {}", event.email(), e.getMessage());
	}
}
