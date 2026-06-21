package com.easytask.project.controller;

import com.easytask.project.dto.AddProjectMemberRequest;
import com.easytask.project.dto.CreateProjectRequest;
import com.easytask.project.dto.ProjectIssueTypeResponse;
import com.easytask.project.dto.ProjectMemberResponse;
import com.easytask.project.dto.ProjectResponse;
import com.easytask.project.dto.StatusResponse;
import com.easytask.auth.entity.User;
import com.easytask.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@AuthenticationPrincipal User currentUser,
                                                    @PathVariable UUID workspaceId,
                                                    @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(currentUser, workspaceId, request));
    }

    @GetMapping
    public List<ProjectResponse> list(@AuthenticationPrincipal User currentUser,
                                       @PathVariable UUID workspaceId) {
        return projectService.listProjects(currentUser, workspaceId);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@AuthenticationPrincipal User currentUser,
                                @PathVariable UUID workspaceId,
                                @PathVariable UUID projectId) {
        return projectService.getProject(currentUser, workspaceId, projectId);
    }

    @GetMapping("/{projectId}/members")
    public List<ProjectMemberResponse> listMembers(@AuthenticationPrincipal User currentUser,
                                                     @PathVariable UUID workspaceId,
                                                     @PathVariable UUID projectId) {
        return projectService.listMembers(currentUser, workspaceId, projectId);
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<ProjectMemberResponse> addMember(@AuthenticationPrincipal User currentUser,
                                                             @PathVariable UUID workspaceId,
                                                             @PathVariable UUID projectId,
                                                             @Valid @RequestBody AddProjectMemberRequest request) {
        var member = projectService.addMember(currentUser, workspaceId, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @GetMapping("/{projectId}/issue-types")
    public List<ProjectIssueTypeResponse> listIssueTypes(@AuthenticationPrincipal User currentUser,
                                                            @PathVariable UUID workspaceId,
                                                            @PathVariable UUID projectId) {
        return projectService.listIssueTypes(currentUser, workspaceId, projectId);
    }

    @GetMapping("/{projectId}/statuses")
    public List<StatusResponse> listStatuses(@AuthenticationPrincipal User currentUser,
                                               @PathVariable UUID workspaceId,
                                               @PathVariable UUID projectId) {
        return projectService.listStatuses(currentUser, workspaceId, projectId);
    }

    @GetMapping("/{projectId}/issue-types/{projectIssueTypeId}/status-options")
    public List<StatusResponse> listStatusOptions(@AuthenticationPrincipal User currentUser,
                                                    @PathVariable UUID workspaceId,
                                                    @PathVariable UUID projectId,
                                                    @PathVariable UUID projectIssueTypeId) {
        return projectService.listStatusOptions(currentUser, workspaceId, projectId, projectIssueTypeId);
    }
}
