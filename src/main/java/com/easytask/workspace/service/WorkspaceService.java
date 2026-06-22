package com.easytask.workspace.service;

import com.easytask.workspace.dto.AddWorkspaceMemberRequest;
import com.easytask.workspace.dto.CreateWorkspaceRequest;
import com.easytask.workspace.dto.WorkspaceMemberResponse;
import com.easytask.workspace.dto.WorkspaceResponse;
import com.easytask.auth.entity.User;
import com.easytask.auth.entity.UserStatus;
import com.easytask.auth.service.AuthService;
import com.easytask.workspace.entity.Workspace;
import com.easytask.workspace.entity.WorkspaceMember;
import com.easytask.workspace.entity.WorkspaceRole;
import com.easytask.workspace.exception.AlreadyWorkspaceMemberException;
import com.easytask.workspace.exception.InsufficientWorkspaceRoleException;
import com.easytask.workspace.exception.InvalidWorkspaceRoleException;
import com.easytask.workspace.exception.WorkspaceNotFoundException;
import com.easytask.auth.repository.UserRepository;
import com.easytask.workspace.repository.WorkspaceMemberRepository;
import com.easytask.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                             WorkspaceMemberRepository workspaceMemberRepository,
                             UserRepository userRepository,
                             AuthService authService) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional
    public WorkspaceResponse createWorkspace(User currentUser, CreateWorkspaceRequest request) {
        Workspace workspace = new Workspace();
        workspace.setName(request.name());
        workspace.setSlug(generateUniqueSlug(request.name()));
        workspace.setOwner(currentUser);
        workspace.setCreatedBy(currentUser);
        workspaceRepository.saveAndFlush(workspace);

        WorkspaceMember membership = new WorkspaceMember();
        membership.setWorkspace(workspace);
        membership.setUser(currentUser);
        membership.setRole(WorkspaceRole.OWNER);
        workspaceMemberRepository.save(membership);

        return toResponse(workspace, WorkspaceRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listMyWorkspaces(User currentUser) {
        return workspaceMemberRepository.findByUser_Id(currentUser.getId()).stream()
                .map(membership -> toResponse(membership.getWorkspace(), membership.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspace(User currentUser, UUID workspaceId) {
        WorkspaceMember membership = requireMembership(currentUser, workspaceId);
        return toResponse(membership.getWorkspace(), membership.getRole());
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listMembers(User currentUser, UUID workspaceId) {
        requireMembership(currentUser, workspaceId);
        return workspaceMemberRepository.findByWorkspace_Id(workspaceId).stream()
                .map(m -> new WorkspaceMemberResponse(
                        m.getUser().getId(), m.getUser().getEmail(), m.getUser().getDisplayName(),
                        m.getRole(), m.getUser().getStatus(), m.getCreatedAt(), null))
                .toList();
    }

    @Transactional
    public WorkspaceMemberResponse addMember(User currentUser, UUID workspaceId, AddWorkspaceMemberRequest request) {
        WorkspaceMember actingMembership = requireMembership(currentUser, workspaceId);
        if (actingMembership.getRole() == WorkspaceRole.MEMBER) {
            throw new InsufficientWorkspaceRoleException();
        }
        if (request.role() == WorkspaceRole.OWNER) {
            throw new InvalidWorkspaceRoleException("Ownership cannot be assigned through this endpoint");
        }

        User targetUser = userRepository.findByEmail(request.email())
                .orElseGet(() -> authService.createPendingUser(request.email(), request.displayName()));

        if (workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(workspaceId, targetUser.getId())) {
            throw new AlreadyWorkspaceMemberException();
        }

        WorkspaceMember membership = new WorkspaceMember();
        membership.setWorkspace(actingMembership.getWorkspace());
        membership.setUser(targetUser);
        membership.setRole(request.role());
        membership.setInvitedBy(currentUser);
        workspaceMemberRepository.saveAndFlush(membership);

        String inviteTokenToReturn = targetUser.getStatus() == UserStatus.PENDING ? targetUser.getInviteToken() : null;
        return new WorkspaceMemberResponse(targetUser.getId(), targetUser.getEmail(),
                targetUser.getDisplayName(), membership.getRole(), targetUser.getStatus(),
                membership.getCreatedAt(), inviteTokenToReturn);
    }

    private WorkspaceMember requireMembership(User currentUser, UUID workspaceId) {
        return workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, currentUser.getId())
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    private String generateUniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isEmpty()) {
            base = "workspace";
        }
        String candidate = base;
        int suffix = 2;
        while (workspaceRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private WorkspaceResponse toResponse(Workspace workspace, WorkspaceRole myRole) {
        return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getSlug(),
                workspace.getOwner().getId(), myRole, workspace.getCreatedAt());
    }
}
