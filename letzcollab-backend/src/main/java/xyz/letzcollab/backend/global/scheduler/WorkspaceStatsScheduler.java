package xyz.letzcollab.backend.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.repository.WorkspaceStatsSnapshotRepository;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceStatsScheduler {

	private final WorkspaceStatsSnapshotRepository snapshotRepository;

	private static final long FIXED_DELAY = 4 * 60 * 1000L; // 직전 스냅샷 생성 실행 종료 후 4분 뒤 실행 (실행 시간이 길어져도 중복 실행 방지)
	private static final long INITIAL_DELAY = 30 * 1000L; // 애플리케이션 시작 후 30초 뒤 첫 실행 (부팅 직후 부하 회피)

	@Scheduled(fixedDelay = FIXED_DELAY, initialDelay = INITIAL_DELAY)
	@Transactional
	void updateSnapshots() {
		long start = System.currentTimeMillis();
		log.info("워크스페이스 통계 스냅샷 생성/수정 시도");
		try {
			snapshotRepository.updateSnapshots(LocalDate.now());
			long end = System.currentTimeMillis() - start;
			log.info("워크스페이스 통계 스냅샷 생성/수정 성공, 소요시간: {}ms", end);
		} catch (Exception e) {
			log.error("워크스페이스 통계 스냅샷 생성/수정 실패", e);
		}
	}
}
