package com.easytask.project.repository;

import com.easytask.project.entity.ProjectIssueType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectIssueTypeRepository extends JpaRepository<ProjectIssueType, UUID> {

    List<ProjectIssueType> findByProject_IdOrderByPosition(UUID projectId);

    Optional<ProjectIssueType> findByIdAndProject_Id(UUID id, UUID projectId);
}
