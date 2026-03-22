package xyz.letzcollab.backend.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import xyz.letzcollab.backend.dto.task.MyTaskSearchCond;
import xyz.letzcollab.backend.dto.task.TaskSearchCond;
import xyz.letzcollab.backend.entity.QUser;
import xyz.letzcollab.backend.entity.Task;
import xyz.letzcollab.backend.entity.vo.TaskPriority;
import xyz.letzcollab.backend.entity.vo.TaskStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static xyz.letzcollab.backend.entity.QProject.project;
import static xyz.letzcollab.backend.entity.QTask.task;
import static xyz.letzcollab.backend.entity.QWorkspace.workspace;

@RequiredArgsConstructor
public class TaskRepositoryCustomImpl implements TaskRepositoryCustom {

	private final JPAQueryFactory query;

	@Override
	public Page<Task> findTasksByCondition(UUID projectPublicId, TaskSearchCond cond, Pageable pageable) {
		QUser assigneeUser = new QUser("assigneeUser");
		QUser reporterUser = new QUser("reporterUser");

		List<Task> tasks = query.selectFrom(task)
								.join(task.assignee, assigneeUser).fetchJoin()
								.join(task.reporter, reporterUser).fetchJoin()
								.leftJoin(task.parentTask).fetchJoin()
								.where(
										task.project.publicId.eq(projectPublicId),
										statusEq(cond.status()),
										priorityEq(cond.priority()),
										assigneeEq(cond.assigneePublicId()),
										dueDateGoe(cond.dueDateFrom()),
										dueDateLoe(cond.dueDateTo())
								)
								.offset(pageable.getOffset())
								.limit(pageable.getPageSize())
								.orderBy(getDefaultOrder())
								.fetch();

		Long total = query.select(task.count())
						  .from(task)
						  .where(
								  task.project.publicId.eq(projectPublicId),
								  statusEq(cond.status()),
								  priorityEq(cond.priority()),
								  assigneeEq(cond.assigneePublicId()),
								  dueDateGoe(cond.dueDateFrom()),
								  dueDateLoe(cond.dueDateTo())
						  ).fetchOne();

		return new PageImpl<>(tasks, pageable, total == null ? 0L : total);
	}

	@Override
	public Optional<Task> findTaskDetailsByPublicId(UUID taskPublicId) {
		QUser assigneeUser = new QUser("assigneeUser");
		QUser reporterUser = new QUser("reporterUser");

		return Optional.ofNullable(
				query.selectFrom(task)
					 .join(task.assignee, assigneeUser).fetchJoin()
					 .join(task.reporter, reporterUser).fetchJoin()
					 .leftJoin(task.parentTask).fetchJoin()
					 .leftJoin(task.subTasks).fetchJoin()
					 .where(task.publicId.eq(taskPublicId))
					 .fetchOne()
		);
	}

	@Override
	public Page<Task> findMyTasks(UUID assigneePublicId, MyTaskSearchCond cond, Pageable pageable) {

		List<Task> tasks = query.selectFrom(task)
								.join(task.project, project).fetchJoin()
								.join(project.workspace, workspace)
								.where(
										task.assignee.publicId.eq(assigneePublicId),
										workspace.publicId.eq(cond.workspacePublicId()),
										statusEq(cond.status()),
										priorityEq(cond.priority()),
										dueDateGoe(cond.dueDateFrom()),
										dueDateLoe(cond.dueDateTo())
								)
								.offset(pageable.getOffset())
								.limit(pageable.getPageSize())
								.orderBy(getDefaultOrder())
								.fetch();

		Long total = query.select(task.count())
						  .from(task)
						  .join(task.project, project)
						  .join(project.workspace, workspace)
						  .where(
								  task.assignee.publicId.eq(assigneePublicId),
								  workspace.publicId.eq(cond.workspacePublicId()),
								  statusEq(cond.status()),
								  priorityEq(cond.priority()),
								  dueDateGoe(cond.dueDateFrom()),
								  dueDateLoe(cond.dueDateTo())
						  ).fetchOne();

		return new PageImpl<>(tasks, pageable, total == null ? 0L : total);
	}

	private BooleanExpression statusEq(TaskStatus status) {
		return status == null ? null : task.status.eq(status);
	}

	private BooleanExpression priorityEq(TaskPriority priority) {
		return priority == null ? null : task.priority.eq(priority);
	}

	private BooleanExpression assigneeEq(UUID assigneePublicId) {
		return assigneePublicId == null ? null : task.assignee.publicId.eq(assigneePublicId);
	}

	private BooleanExpression dueDateGoe(LocalDate from) {
		return from == null ? null : task.dueDate.goe(from);
	}

	private BooleanExpression dueDateLoe(LocalDate to) {
		return to == null ? null : task.dueDate.loe(to);
	}

	/**
	 * 기본 정렬 기준
	 * 1. 우선순위 내림차순 (긴급한 것 먼저)
	 * 2. 기한 오름차순  (임박한 것 먼저)
	 * 3. 생성일 내림차순 (최신 먼저)
	 */
	private OrderSpecifier<?>[] getDefaultOrder() {
		return new OrderSpecifier[]{
				new OrderSpecifier<>(Order.DESC, task.priority),
				new OrderSpecifier<>(Order.ASC, task.dueDate),
				new OrderSpecifier<>(Order.DESC, task.createdAt)
		};
	}
}