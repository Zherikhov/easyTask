package com.easytask.issue.repository;

import com.easytask.issue.entity.IssueComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssueCommentRepository extends JpaRepository<IssueComment, UUID> {

    Optional<IssueComment> findByIdAndIssue_IdAndDeletedAtIsNull(UUID id, UUID issueId);

    List<IssueComment> findByIssue_IdAndDeletedAtIsNullOrderByCreatedAt(UUID issueId);
}
