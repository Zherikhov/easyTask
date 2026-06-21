package com.easytask.issue.service;

import com.easytask.auth.entity.User;
import com.easytask.issue.dto.CreateCommentRequest;
import com.easytask.issue.dto.IssueCommentResponse;
import com.easytask.issue.dto.UpdateCommentRequest;
import com.easytask.issue.entity.Issue;
import com.easytask.issue.entity.IssueComment;
import com.easytask.issue.exception.CommentAccessDeniedException;
import com.easytask.issue.exception.CommentNotFoundException;
import com.easytask.issue.exception.IssueNotFoundException;
import com.easytask.issue.repository.IssueCommentRepository;
import com.easytask.issue.repository.IssueRepository;
import com.easytask.project.entity.Project;
import com.easytask.project.entity.ProjectRole;
import com.easytask.project.exception.ProjectAccessDeniedException;
import com.easytask.project.exception.ProjectNotFoundException;
import com.easytask.project.repository.ProjectMemberRepository;
import com.easytask.project.repository.ProjectRepository;
import com.easytask.workspace.entity.WorkspaceMember;
import com.easytask.workspace.entity.WorkspaceRole;
import com.easytask.workspace.exception.WorkspaceNotFoundException;
import com.easytask.workspace.repository.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class IssueCommentService {

    private final IssueCommentRepository issueCommentRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public IssueCommentService(IssueCommentRepository issueCommentRepository,
                                IssueRepository issueRepository,
                                ProjectRepository projectRepository,
                                ProjectMemberRepository projectMemberRepository,
                                WorkspaceMemberRepository workspaceMemberRepository) {
        this.issueCommentRepository = issueCommentRepository;
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public IssueCommentResponse createComment(User currentUser, UUID workspaceId, UUID projectId, UUID issueId,
                                               CreateCommentRequest request) {
        WorkspaceMember actingMembership = requireWorkspaceMembership(currentUser, workspaceId);
        requireProject(workspaceId, projectId);
        requireProjectWriteAccess(actingMembership, projectId, currentUser);
        Issue issue = requireIssue(projectId, issueId);

        IssueComment comment = new IssueComment();
        comment.setIssue(issue);
        comment.setAuthor(currentUser);
        comment.setBody(request.body());
        issueCommentRepository.saveAndFlush(comment);

        return toResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<IssueCommentResponse> listComments(User currentUser, UUID workspaceId, UUID projectId, UUID issueId) {
        requireWorkspaceMembership(currentUser, workspaceId);
        requireProject(workspaceId, projectId);
        requireIssue(projectId, issueId);
        return issueCommentRepository.findByIssue_IdAndDeletedAtIsNullOrderByCreatedAt(issueId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public IssueCommentResponse updateComment(User currentUser, UUID workspaceId, UUID projectId, UUID issueId,
                                               UUID commentId, UpdateCommentRequest request) {
        requireWorkspaceMembership(currentUser, workspaceId);
        requireProject(workspaceId, projectId);
        requireIssue(projectId, issueId);
        IssueComment comment = requireComment(issueId, commentId);
        requireAuthor(comment, currentUser);

        comment.setBody(request.body());
        comment.setEditedAt(OffsetDateTime.now());
        issueCommentRepository.saveAndFlush(comment);

        return toResponse(comment);
    }

    @Transactional
    public void deleteComment(User currentUser, UUID workspaceId, UUID projectId, UUID issueId, UUID commentId) {
        requireWorkspaceMembership(currentUser, workspaceId);
        requireProject(workspaceId, projectId);
        requireIssue(projectId, issueId);
        IssueComment comment = requireComment(issueId, commentId);
        requireAuthor(comment, currentUser);

        comment.setDeletedAt(OffsetDateTime.now());
        issueCommentRepository.save(comment);
    }

    private void requireAuthor(IssueComment comment, User currentUser) {
        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new CommentAccessDeniedException();
        }
    }

    private void requireProjectWriteAccess(WorkspaceMember actingMembership, UUID projectId, User currentUser) {
        boolean canWrite = actingMembership.getRole() != WorkspaceRole.MEMBER
                || projectMemberRepository.findByProject_IdAndUser_Id(projectId, currentUser.getId())
                        .map(m -> m.getRole() != ProjectRole.VIEWER)
                        .orElse(false);
        if (!canWrite) {
            throw new ProjectAccessDeniedException();
        }
    }

    private WorkspaceMember requireWorkspaceMembership(User currentUser, UUID workspaceId) {
        return workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, currentUser.getId())
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    private Project requireProject(UUID workspaceId, UUID projectId) {
        return projectRepository.findByIdAndWorkspace_IdAndDeletedAtIsNull(projectId, workspaceId)
                .orElseThrow(ProjectNotFoundException::new);
    }

    private Issue requireIssue(UUID projectId, UUID issueId) {
        return issueRepository.findByIdAndProject_IdAndDeletedAtIsNull(issueId, projectId)
                .orElseThrow(IssueNotFoundException::new);
    }

    private IssueComment requireComment(UUID issueId, UUID commentId) {
        return issueCommentRepository.findByIdAndIssue_IdAndDeletedAtIsNull(commentId, issueId)
                .orElseThrow(CommentNotFoundException::new);
    }

    private IssueCommentResponse toResponse(IssueComment comment) {
        return new IssueCommentResponse(
                comment.getId(),
                comment.getIssue().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getDisplayName(),
                comment.getBody(),
                comment.getEditedAt(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
