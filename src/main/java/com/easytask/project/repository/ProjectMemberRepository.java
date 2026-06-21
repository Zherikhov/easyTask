package com.easytask.project.repository;

import com.easytask.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    Optional<ProjectMember> findByProject_IdAndUser_Id(UUID projectId, UUID userId);

    boolean existsByProject_IdAndUser_Id(UUID projectId, UUID userId);

    List<ProjectMember> findByProject_Id(UUID projectId);

    List<ProjectMember> findByUser_Id(UUID userId);
}
