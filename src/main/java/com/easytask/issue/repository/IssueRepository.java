package com.easytask.issue.repository;

import com.easytask.issue.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssueRepository extends JpaRepository<Issue, UUID> {

    Optional<Issue> findByIdAndProject_IdAndDeletedAtIsNull(UUID id, UUID projectId);

    Optional<Issue> findByIdAndProject_IdAndStatus_IdAndDeletedAtIsNull(UUID id, UUID projectId, UUID statusId);

    List<Issue> findByProject_IdAndDeletedAtIsNullOrderByNumber(UUID projectId);

    @Query("SELECT MAX(i.position) FROM Issue i WHERE i.project.id = :projectId AND i.status.id = :statusId AND i.deletedAt IS NULL")
    BigDecimal findMaxPosition(@Param("projectId") UUID projectId, @Param("statusId") UUID statusId);
}
