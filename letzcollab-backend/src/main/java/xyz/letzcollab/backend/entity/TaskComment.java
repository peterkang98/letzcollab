package xyz.letzcollab.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.letzcollab.backend.global.entity.DateBaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "task_comments",
	indexes = {
		@Index(
			name = "idx_task_comments_task_parent_created",
			columnList = "task_id, parent_comment_id, created_at"
		),
		// childComments 조인용
		@Index(name = "idx_task_comments_parent",
				columnList = "parent_comment_id")
	}
)
public class TaskComment extends DateBaseEntity {

	@Id
	@GeneratedValue
	@Column(name = "comment_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id", nullable = false, updatable = false)
	private Task task;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false, updatable = false)
	private User author;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_comment_id")
	private TaskComment parentComment;

	@OneToMany(mappedBy = "parentComment", cascade = CascadeType.REMOVE)
	private List<TaskComment> childComments = new ArrayList<>();

	@Builder(access = AccessLevel.PRIVATE)
	private TaskComment(Task task, User author, String content, TaskComment parentComment) {
		this.task = task;
		this.author = author;
		this.content = content;
		this.parentComment = parentComment;
	}

	public static TaskComment createComment(Task task, User author, String content) {
		return TaskComment.builder()
						  .task(task)
						  .author(author)
						  .content(content)
						  .build();
	}

	public static TaskComment createReply(Task task, User author, String content, TaskComment parentComment) {
		return TaskComment.builder()
						  .task(task)
						  .author(author)
						  .content(content)
						  .parentComment(parentComment)
						  .build();
	}

	public void updateContent(String newContent) {
		this.content = newContent;
	}
}
