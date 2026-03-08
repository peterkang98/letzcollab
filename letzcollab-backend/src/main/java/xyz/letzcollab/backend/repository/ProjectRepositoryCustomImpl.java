package xyz.letzcollab.backend.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import xyz.letzcollab.backend.dto.project.ProjectSearchCond;
import xyz.letzcollab.backend.entity.Project;
import xyz.letzcollab.backend.entity.QUser;
import xyz.letzcollab.backend.entity.vo.ProjectStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static xyz.letzcollab.backend.entity.QProject.project;
import static xyz.letzcollab.backend.entity.QProjectMember.projectMember;
import static xyz.letzcollab.backend.entity.QUser.user;
import static xyz.letzcollab.backend.entity.QWorkspace.workspace;
import static xyz.letzcollab.backend.entity.QWorkspaceMember.workspaceMember;

@RequiredArgsConstructor
public class ProjectRepositoryCustomImpl implements ProjectRepositoryCustom {
	private final JPAQueryFactory query;

	@Override
	public Page<Project> findProjectsByCondition(
			UUID userPublicId, UUID workspacePublicId, ProjectSearchCond cond, Pageable pageable
	) {
		List<Project> projects = query.selectFrom(project)
									  .join(project.workspace, workspace)
									  .leftJoin(project.leader, user).fetchJoin()    // 혹시 leader가 null이 들어간 경우에도 프로젝트는 조회 가능하도록
									  .where(
										  nameContains(cond.keyword()),
										  statusEq(cond.status()),
										  isAccessible(userPublicId)
									  )
									  .offset(pageable.getOffset())
									  .limit(pageable.getPageSize())
									  .orderBy(getOrderSpecifiers(pageable))
									  .fetch();

		Long total = query.select(project.count())
						  .from(project)
						  .where(
							  project.workspace.publicId.eq(workspacePublicId),
							  isAccessible(userPublicId)
						  ).fetchOne();

		return new PageImpl<>(projects, pageable, total == null ? 0L : total);
	}

	@Override
	public Optional<Project> findProjectDetailsByPublicIds(UUID userPublicId, UUID workspacePublicId, UUID projectPublicId) {
		QUser leaderUser = new QUser("leaderUser");
		QUser memberUser = new QUser("memberUser");

		return Optional.ofNullable(
				query.selectFrom(project)
					 .leftJoin(project.leader, leaderUser).fetchJoin()
					 .leftJoin(project.members, projectMember).fetchJoin()
					 .leftJoin(projectMember.user, memberUser).fetchJoin()
					 .join(project.workspace, workspace)
					 .join(workspace.members, workspaceMember)
					 .where(
						 workspace.publicId.eq(workspacePublicId),
						 project.publicId.eq(projectPublicId),
						 workspaceMember.user.publicId.eq(userPublicId),
						 isAccessible(userPublicId)
					 )
					 .fetchOne()
		);
	}

	private BooleanExpression nameContains(String keyword) {
		return StringUtils.hasText(keyword) ? project.name.contains(keyword) : null;
	}

	private BooleanExpression statusEq(ProjectStatus status) {
		return status == null ? null : project.status.eq(status);
	}

	// 공개 프로젝트이거나 내가 프로젝트 멤버이거나
	private BooleanExpression isAccessible(UUID userPublicId) {
		return project.isPrivate.isFalse()
								.or(project.members.any().user.publicId.eq(userPublicId));
	}

	private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
		List<OrderSpecifier<?>> orders = new ArrayList<>();
		PathBuilder<Project> pathBuilder = new PathBuilder<>(project.getType(), project.getMetadata()); // 문자열로 된 필드 이름을 실제 Querydsl 객체로 바꿔주는 동적 매퍼

		if (!pageable.getSort().isEmpty()) {
			for (Sort.Order order : pageable.getSort()) {
				Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;

				// 허용된 필드만 정렬 가능
				if (List.of("name", "createdAt", "status").contains(order.getProperty())) {
					orders.add(new OrderSpecifier<>(direction, pathBuilder.get(order.getProperty(), Comparable.class)));
				}
			}
		} else {
			orders.add(new OrderSpecifier<>(Order.DESC, project.createdAt));
		}

		return orders.toArray(OrderSpecifier[]::new);
	}
}
