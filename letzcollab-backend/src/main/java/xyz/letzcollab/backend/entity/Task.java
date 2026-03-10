package xyz.letzcollab.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import xyz.letzcollab.backend.entity.vo.TaskPriority;
import xyz.letzcollab.backend.entity.vo.TaskStatus;
import xyz.letzcollab.backend.global.entity.PublicIdAndFullAuditBaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tasks")
public class Task extends PublicIdAndFullAuditBaseEntity {

	@Id
	@GeneratedValue
	@Column(name = "task_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(nullable = false)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(length = 20, nullable = false)
	private TaskStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id", nullable = false)
	private User assignee;

	@Enumerated(EnumType.STRING)
	@Column(length = 20, nullable = false)
	private TaskPriority priority;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_task_id")
	private Task parentTask;

	@OneToMany(mappedBy = "parentTask")
	private List<Task> subTasks = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporter_id", nullable = false)
	private User reporter;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Builder(access = AccessLevel.PRIVATE)
	private Task(Project project, String name, String description, TaskStatus status,
				 User assignee, TaskPriority priority, Task parentTask, User reporter, LocalDate dueDate) {
		this.project = project;
		this.name = name;
		this.description = description;
		this.status = status;
		this.assignee = assignee;
		this.priority = priority;
		this.parentTask = parentTask;
		this.reporter = reporter;
		this.dueDate = dueDate;
	}

	public static Task createTask(Project project, String name, String description, User assignee,
								  TaskPriority priority, Task parentTask, User reporter, LocalDate dueDate) {

		return Task.builder()
				   .project(project)
				   .name(name)
				   .description(description)
				   .status(TaskStatus.TODO)
				   .assignee(assignee)
				   .priority(priority)
				   .parentTask(parentTask)
				   .reporter(reporter)
				   .dueDate(dueDate)
				   .build();
	}

	public void update(String name, String description, TaskStatus status,
					   User assignee, TaskPriority priority, LocalDate dueDate) {
		if (name != null) this.name = name;
		if (description != null) this.description = description;
		if (status != null) this.status = status;
		if (assignee != null) this.assignee = assignee;
		if (priority != null) this.priority = priority;
		if (dueDate != null) this.dueDate = dueDate;
	}

	// N+1이 발생하기는 하지만, 앱 전체적으로 삭제 빈도가 높지 않고, depth도 현실적으로 2~3정도일거 같아서 이런 재귀 방식으로 둠
	public void softDelete() {
		this.subTasks.forEach(Task::softDelete);
		this.deletedAt = LocalDateTime.now();
	}

	// 상태 복구 기능 미제공 — 부모 CANCELLED시 하위 업무들도 전부 연쇄 취소
	public void cancelWithSubTasks() {
		this.subTasks.forEach(Task::cancelWithSubTasks);
		this.status = TaskStatus.CANCELLED;
	}
}
