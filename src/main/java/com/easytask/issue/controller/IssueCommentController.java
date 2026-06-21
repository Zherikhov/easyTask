package com.easytask.issue.controller;

import com.easytask.auth.entity.User;
import com.easytask.issue.dto.CreateCommentRequest;
import com.easytask.issue.dto.IssueCommentResponse;
import com.easytask.issue.dto.UpdateCommentRequest;
import com.easytask.issue.service.IssueCommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/projects/{projectId}/issues/{issueId}/comments")
public class IssueCommentController {

    private final IssueCommentService issueCommentService;

    public IssueCommentController(IssueCommentService issueCommentService) {
        this.issueCommentService = issueCommentService;
    }

    @PostMapping
    public ResponseEntity<IssueCommentResponse> create(@AuthenticationPrincipal User currentUser,
                                                         @PathVariable UUID workspaceId,
                                                         @PathVariable UUID projectId,
                                                         @PathVariable UUID issueId,
                                                         @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(issueCommentService.createComment(currentUser, workspaceId, projectId, issueId, request));
    }

    @GetMapping
    public List<IssueCommentResponse> list(@AuthenticationPrincipal User currentUser,
                                            @PathVariable UUID workspaceId,
                                            @PathVariable UUID projectId,
                                            @PathVariable UUID issueId) {
        return issueCommentService.listComments(currentUser, workspaceId, projectId, issueId);
    }

    @PatchMapping("/{commentId}")
    public IssueCommentResponse update(@AuthenticationPrincipal User currentUser,
                                        @PathVariable UUID workspaceId,
                                        @PathVariable UUID projectId,
                                        @PathVariable UUID issueId,
                                        @PathVariable UUID commentId,
                                        @Valid @RequestBody UpdateCommentRequest request) {
        return issueCommentService.updateComment(currentUser, workspaceId, projectId, issueId, commentId, request);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User currentUser,
                                        @PathVariable UUID workspaceId,
                                        @PathVariable UUID projectId,
                                        @PathVariable UUID issueId,
                                        @PathVariable UUID commentId) {
        issueCommentService.deleteComment(currentUser, workspaceId, projectId, issueId, commentId);
        return ResponseEntity.noContent().build();
    }
}
