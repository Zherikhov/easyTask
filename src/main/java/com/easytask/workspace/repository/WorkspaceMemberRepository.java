package com.easytask.workspace.repository;

import com.easytask.workspace.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    Optional<WorkspaceMember> findByWorkspace_IdAndUser_Id(UUID workspaceId, UUID userId);

    boolean existsByWorkspace_IdAndUser_Id(UUID workspaceId, UUID userId);

    List<WorkspaceMember> findByUser_Id(UUID userId);

    List<WorkspaceMember> findByWorkspace_Id(UUID workspaceId);
}
