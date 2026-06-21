package com.easytask.project.repository;

import com.easytask.project.entity.ProjectIssueTypeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectIssueTypeStatusRepository extends JpaRepository<ProjectIssueTypeStatus, UUID> {

    List<ProjectIssueTypeStatus> findByProjectIssueType_Id(UUID projectIssueTypeId);

    List<ProjectIssueTypeStatus> findByProjectIssueType_IdOrderByPosition(UUID projectIssueTypeId);

    Optional<ProjectIssueTypeStatus> findFirstByProjectIssueType_IdOrderByPosition(UUID projectIssueTypeId);

    Optional<ProjectIssueTypeStatus> findByProjectIssueType_IdAndStatus_Id(UUID projectIssueTypeId, UUID statusId);
}
