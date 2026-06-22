package com.easytask.project.service;

import com.easytask.project.dto.AddProjectMemberRequest;
import com.easytask.project.dto.CreateProjectRequest;
import com.easytask.project.dto.ProjectIssueTypeResponse;
import com.easytask.project.dto.ProjectMemberResponse;
import com.easytask.project.dto.ProjectResponse;
import com.easytask.project.dto.StatusResponse;
import com.easytask.project.entity.Project;
import com.easytask.project.entity.ProjectIssueType;
import com.easytask.project.entity.ProjectMember;
import com.easytask.project.entity.ProjectRole;
import com.easytask.project.entity.Status;
import com.easytask.auth.entity.User;
import com.easytask.workspace.entity.WorkspaceMember;
import com.easytask.workspace.entity.WorkspaceRole;
import com.easytask.project.exception.AlreadyProjectMemberException;
import com.easytask.project.exception.InsufficientProjectRoleException;
import com.easytask.project.exception.InvalidIssueTypeException;
import com.easytask.project.exception.InvalidProjectKeyException;
import com.easytask.project.exception.ProjectKeyAlreadyUsedException;
import com.easytask.project.exception.ProjectNotFoundException;
import com.easytask.common.exception.UserNotFoundException;
import com.easytask.project.exception.UserNotWorkspaceMemberException;
import com.easytask.workspace.exception.WorkspaceNotFoundException;
import com.easytask.project.repository.ProjectIssueTypeRepository;
import com.easytask.project.repository.ProjectIssueTypeStatusRepository;
import com.easytask.project.repository.ProjectMemberRepository;
import com.easytask.project.repository.ProjectRepository;
import com.easytask.project.repository.StatusRepository;
import com.easytask.auth.repository.UserRepository;
import com.easytask.workspace.repository.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9]{1,9}$");

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectIssueTypeRepository projectIssueTypeRepository;
    private final ProjectIssueTypeStatusRepository projectIssueTypeStatusRepository;
    private final StatusRepository statusRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final ProjectDefaultsSeeder projectDefaultsSeeder;

    public ProjectService(ProjectRepository projectRepository,
                           ProjectMemberRepository projectMemberRepository,
                           ProjectIssueTypeRepository projectIssueTypeRepository,
                           ProjectIssueTypeStatusRepository projectIssueTypeStatusRepository,
                           StatusRepository statusRepository,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           UserRepository userRepository,
                           ProjectDefaultsSeeder projectDefaultsSeeder) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectIssueTypeRepository = projectIssueTypeRepository;
        this.projectIssueTypeStatusRepository = projectIssueTypeStatusRepository;
        this.statusRepository = statusRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.projectDefaultsSeeder = projectDefaultsSeeder;
    }

    @Transactional
    public ProjectResponse createProject(User currentUser, UUID workspaceId, CreateProjectRequest request) {
        WorkspaceMember actingMembership = requireWorkspaceMembership(currentUser, workspaceId);

        String key = request.key().toUpperCase(Locale.ROOT);
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new InvalidProjectKeyException();
        }
        if (projectRepository.existsByWorkspace_IdAndKeyAndDeletedAtIsNull(workspaceId, key)) {
            throw new ProjectKeyAlreadyUsedException(key);
        }

        Project project = new Project();
        project.setWorkspace(actingMembership.getWorkspace());
        project.setKey(key);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setCreatedBy(currentUser);
        projectRepository.saveAndFlush(project);

        ProjectMember membership = new ProjectMember();
        membership.setProject(project);
        membership.setUser(currentUser);
        membership.setRole(ProjectRole.LEAD);
        projectMemberRepository.save(membership);

        projectDefaultsSeeder.seed(project, currentUser);

        return toResponse(project, ProjectRole.LEAD);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(User currentUser, UUID workspaceId) {
        requireWorkspaceMembership(currentUser, workspaceId);
        List<Project> projects = projectRepository.findByWorkspace_IdAndDeletedAtIsNull(workspaceId);
        Map<UUID, ProjectRole> myRoles = myProjectRoles(currentUser);
        return projects.stream()
                .map(p -> toResponse(p, myRoles.get(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(User currentUser, UUID workspaceId, UUID projectId) {
        requireWorkspaceMembership(currentUser, workspaceId);
        Project project = requireProject(workspaceId, projectId);
        ProjectRole myRole = projectMemberRepository.findByProject_IdAndUser_Id(projectId, currentUser.getId())
                .map(ProjectMember::getRole)
                .orElse(null);
        return toResponse(project, myRole);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(User currentUser, UUID workspaceId, UUID projectId) {
        requireWorkspaceMembership(currentUser, workspaceId);
        requireProject(workspaceId, projectId);
        return projectMemberRepository.findByProject_Id(projectId).stream()
                .map(m -> new ProjectMemberResponse(
                        m.getUser().getId(), m.getUser().getEmail(), m.getUser().getDisplayName(),
                        m.getRole(), m.getUser().getStatus(), m.getCreatedAt()))
                .toList();
    }

    @Transactional
    public ProjectMemberResponse addMember(User currentUser, UUID workspaceId, UUID projectId,
                                            AddProjectMemberRequest request) {
        WorkspaceMember actingWorkspaceMembership = requireWorkspaceMembership(currentUser, workspaceId);
        Project project = requireProject(workspaceId, projectId);

        boolean canManage = actingWorkspaceMembership.getRole() != WorkspaceRole.MEMBER
                || projectMemberRepository.findByProject_IdAndUser_Id(projectId, currentUser.getId())
                        .map(m -> m.getRole() == ProjectRole.LEAD)
                        .orElse(false);
        if (!canManage) {
            throw new InsufficientProjectRoleException();
        }

        User targetUser = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException(request.email()));

        if (!workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(workspaceId, targetUser.getId())) {
            throw new UserNotWorkspaceMemberException();
        }
        if (projectMemberRepository.existsByProject_IdAndUser_Id(projectId, targetUser.getId())) {
            throw new AlreadyProjectMemberException();
        }

        ProjectMember membership = new ProjectMember();
        membership.setProject(project);
        membership.setUser(targetUser);
        membership.setRole(request.role());
        projectMemberRepository.saveAndFlush(membership);

        return new ProjectMemberResponse(targetUser.getId(), targetUser.getEmail(),
                targetUser.getDisplayName(), membership.getRole(), targetUser.getStatus(), membership.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ProjectIssueTypeResponse> listIssueTypes(User currentUser, UUID workspaceId, UUID projectId) {
        requireWorkspaceMembership(currentUser, workspaceId);
        requireProject(workspaceId, projectId);
        return projectIssueTypeRepository.findByProject_IdOrderByPosition(projectId).stream()
                .map(pit -> new ProjectIssueTypeResponse(pit.getId(), pit.getIssueType().getName(), pit.getPosition()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StatusResponse> listStatuses(User currentUser, UUID workspaceId, UUID projectId) {
        requireWorkspaceMembership(currentUser, workspaceId);
        requireProject(workspaceId, projectId);
        return statusRepository.findByProject_IdOrderByPosition(projectId).stream()
                .map(s -> new StatusResponse(s.getId(), s.getName(), s.getCategory(), s.getPosition()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StatusResponse> listStatusOptions(User currentUser, UUID workspaceId, UUID projectId,
                                                    UUID projectIssueTypeId) {
        requireWorkspaceMembership(currentUser, workspaceId);
        requireProject(workspaceId, projectId);
        ProjectIssueType projectIssueType = projectIssueTypeRepository
                .findByIdAndProject_Id(projectIssueTypeId, projectId)
                .orElseThrow(InvalidIssueTypeException::new);
        return projectIssueTypeStatusRepository.findByProjectIssueType_IdOrderByPosition(projectIssueType.getId())
                .stream()
                .map(pits -> {
                    Status s = pits.getStatus();
                    return new StatusResponse(s.getId(), s.getName(), s.getCategory(), s.getPosition());
                })
                .toList();
    }

    private WorkspaceMember requireWorkspaceMembership(User currentUser, UUID workspaceId) {
        return workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, currentUser.getId())
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    private Project requireProject(UUID workspaceId, UUID projectId) {
        return projectRepository.findByIdAndWorkspace_IdAndDeletedAtIsNull(projectId, workspaceId)
                .orElseThrow(ProjectNotFoundException::new);
    }

    private Map<UUID, ProjectRole> myProjectRoles(User currentUser) {
        return projectMemberRepository.findByUser_Id(currentUser.getId()).stream()
                .collect(Collectors.toMap(m -> m.getProject().getId(), ProjectMember::getRole));
    }

    private ProjectResponse toResponse(Project project, ProjectRole myRole) {
        return new ProjectResponse(project.getId(), project.getWorkspace().getId(), project.getKey(),
                project.getName(), project.getDescription(), myRole, project.getCreatedAt());
    }
}
