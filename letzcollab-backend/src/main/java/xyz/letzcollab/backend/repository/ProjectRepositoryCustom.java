package xyz.letzcollab.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import xyz.letzcollab.backend.dto.project.ProjectSearchCond;
import xyz.letzcollab.backend.entity.Project;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepositoryCustom {
	Page<Project> findProjectsByCondition(
			UUID userPublicId, UUID workspacePublicId, ProjectSearchCond cond, Pageable pageable
	);

	Optional<Project> findProjectDetailsByPublicIds(UUID userPublicId, UUID workspacePublicId, UUID projectPublicId);
}
