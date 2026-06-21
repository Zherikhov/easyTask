package com.easytask.workspace.controller;

import com.easytask.workspace.dto.AddWorkspaceMemberRequest;
import com.easytask.workspace.dto.CreateWorkspaceRequest;
import com.easytask.workspace.dto.WorkspaceMemberResponse;
import com.easytask.workspace.dto.WorkspaceResponse;
import com.easytask.auth.entity.User;
import com.easytask.workspace.service.WorkspaceService;
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
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(@AuthenticationPrincipal User currentUser,
                                                      @Valid @RequestBody CreateWorkspaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.createWorkspace(currentUser, request));
    }

    @GetMapping
    public List<WorkspaceResponse> listMine(@AuthenticationPrincipal User currentUser) {
        return workspaceService.listMyWorkspaces(currentUser);
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse get(@AuthenticationPrincipal User currentUser, @PathVariable UUID workspaceId) {
        return workspaceService.getWorkspace(currentUser, workspaceId);
    }

    @GetMapping("/{workspaceId}/members")
    public List<WorkspaceMemberResponse> listMembers(@AuthenticationPrincipal User currentUser,
                                                       @PathVariable UUID workspaceId) {
        return workspaceService.listMembers(currentUser, workspaceId);
    }

    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<WorkspaceMemberResponse> addMember(@AuthenticationPrincipal User currentUser,
                                                               @PathVariable UUID workspaceId,
                                                               @Valid @RequestBody AddWorkspaceMemberRequest request) {
        var member = workspaceService.addMember(currentUser, workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }
}
