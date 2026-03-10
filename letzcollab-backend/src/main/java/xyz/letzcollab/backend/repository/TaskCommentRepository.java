package xyz.letzcollab.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.letzcollab.backend.entity.TaskComment;

import java.util.List;
import java.util.Optional;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

	// 특정 업무의 최상위 댓글 목록 조회 - 대댓글 및 작성자 fetch join
	@Query("SELECT c FROM TaskComment c " +
			"JOIN FETCH c.author " +
			"LEFT JOIN FETCH c.childComments children " +
			"LEFT JOIN FETCH children.author " +
			"WHERE c.task.id = :taskId " +
			"AND c.parentComment IS NULL " +
			"ORDER BY c.createdAt ASC")
	List<TaskComment> findTopLevelCommentsWithChildren(@Param("taskId") Long taskId);

	// 수정 권한 검증용 - 작성자 fetch join
	@Query("SELECT c FROM TaskComment c " +
			"JOIN FETCH c.author " +
			"WHERE c.id = :commentId")
	Optional<TaskComment> findByIdWithAuthor(@Param("commentId") Long commentId);

	// 삭제 권한 검증용 - 대댓글 및 작성자 fetch join
	@Query("SELECT c FROM TaskComment c " +
			"JOIN FETCH c.author " +
			"LEFT JOIN FETCH c.childComments " +
			"WHERE c.id = :commentId")
	Optional<TaskComment> findByIdWithAuthorAndChildren(@Param("commentId") Long commentId);
}
