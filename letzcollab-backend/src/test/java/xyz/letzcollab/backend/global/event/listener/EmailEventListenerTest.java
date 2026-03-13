package xyz.letzcollab.backend.global.event.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import xyz.letzcollab.backend.global.email.EmailService;
import xyz.letzcollab.backend.global.email.context.VerifyEmailContext;
import xyz.letzcollab.backend.global.event.dto.EmailEvent;
import xyz.letzcollab.backend.global.exception.CustomException;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static xyz.letzcollab.backend.global.exception.ErrorCode.EMAIL_SEND_ERROR;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("EmailEventListener 통합 테스트")
class EmailEventListenerTest {
	@Autowired
	private ApplicationEventPublisher publisher;

	@Autowired
	TransactionTemplate transactionTemplate;

	@MockitoBean
	private EmailService emailService;

	@Test
	@DisplayName("이메일 발송 이벤트 발생 시 비동기로 이메일 서비스가 호출된다")
	void handleEmailEvent_async_test() {
		// given
		EmailEvent event = new EmailEvent("test@example.com", new VerifyEmailContext("test", "token", "url"));

		// when
		// 트랜잭션 시작 + 커밋. 트랜잭션을 실제 커밋시켜야, @TransactionalEventListener가 실제로 작동함
		transactionTemplate.executeWithoutResult(status -> publisher.publishEvent(event));

		// then
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(emailService, times(1)).sendTemplateEmail(anyString(), any());
		});
	}

	@Test
	@DisplayName("이메일 발송 실패 시 @Retryable 설정에 따라 총 3번 재시도한다")
	void retry_mechanism_test() {
		// given
		EmailEvent event = new EmailEvent("fail@example.com",
				new VerifyEmailContext("fail", "token", "url"));

		// 호출될 때마다 무조건 예외를 던지도록 설정
		doThrow(new CustomException(EMAIL_SEND_ERROR))
				.when(emailService).sendTemplateEmail(anyString(), any());

		// when
		transactionTemplate.executeWithoutResult(status -> {
			publisher.publishEvent(event);
		});

		// then
		// backoff delay가 2s, multiplier 2.0이므로 (초기1회 + 2s대기 + 2회차 + 4s대기 + 3회차)
		// emailService를 mock했으므로 최소 6~7초는 기다려야 모든 재시도가 끝남
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(emailService, times(3)).sendTemplateEmail(anyString(), any());
		});
	}
}