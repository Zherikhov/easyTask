package com.easytask.project.repository;

import com.easytask.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    boolean existsByWorkspace_IdAndKeyAndDeletedAtIsNull(UUID workspaceId, String key);

    Optional<Project> findByIdAndWorkspace_IdAndDeletedAtIsNull(UUID id, UUID workspaceId);

    List<Project> findByWorkspace_IdAndDeletedAtIsNull(UUID workspaceId);

    // Atomic per-project issue counter: the RETURNING clause lets us read back the
    // post-increment value in the same round trip, under the row lock taken by the UPDATE.
    @Query(value = "UPDATE project SET issue_seq = issue_seq + 1 WHERE id = :id RETURNING issue_seq", nativeQuery = true)
    long incrementAndGetIssueSeq(@Param("id") UUID id);
}
