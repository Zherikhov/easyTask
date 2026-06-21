package com.easytask.project.repository;

import com.easytask.project.entity.IssueType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueTypeRepository extends JpaRepository<IssueType, UUID> {

    List<IssueType> findByWorkspace_IdOrderByPosition(UUID workspaceId);
}
