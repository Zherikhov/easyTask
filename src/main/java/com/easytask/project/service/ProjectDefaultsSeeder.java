package com.easytask.project.service;

import com.easytask.project.entity.Board;
import com.easytask.project.entity.BoardColumn;
import com.easytask.project.entity.IssueType;
import com.easytask.project.entity.Project;
import com.easytask.project.entity.ProjectIssueType;
import com.easytask.project.entity.ProjectIssueTypeStatus;
import com.easytask.project.entity.Status;
import com.easytask.project.entity.StatusCategory;
import com.easytask.auth.entity.User;
import com.easytask.project.repository.BoardColumnRepository;
import com.easytask.project.repository.BoardRepository;
import com.easytask.project.repository.IssueTypeRepository;
import com.easytask.project.repository.ProjectIssueTypeRepository;
import com.easytask.project.repository.ProjectIssueTypeStatusRepository;
import com.easytask.project.repository.StatusRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds a brand-new project with a usable issue-type/status/board setup, so issues
 * can be created immediately without a separate taxonomy-configuration step.
 */
@Component
class ProjectDefaultsSeeder {

    private static final List<String> DEFAULT_ISSUE_TYPES = List.of("Task", "Bug", "Story");

    private record DefaultStatus(String name, StatusCategory category) {
    }

    private static final List<DefaultStatus> DEFAULT_STATUSES = List.of(
            new DefaultStatus("To Do", StatusCategory.TODO),
            new DefaultStatus("In Progress", StatusCategory.IN_PROGRESS),
            new DefaultStatus("Done", StatusCategory.DONE)
    );

    private final IssueTypeRepository issueTypeRepository;
    private final StatusRepository statusRepository;
    private final ProjectIssueTypeRepository projectIssueTypeRepository;
    private final ProjectIssueTypeStatusRepository projectIssueTypeStatusRepository;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;

    ProjectDefaultsSeeder(IssueTypeRepository issueTypeRepository,
                           StatusRepository statusRepository,
                           ProjectIssueTypeRepository projectIssueTypeRepository,
                           ProjectIssueTypeStatusRepository projectIssueTypeStatusRepository,
                           BoardRepository boardRepository,
                           BoardColumnRepository boardColumnRepository) {
        this.issueTypeRepository = issueTypeRepository;
        this.statusRepository = statusRepository;
        this.projectIssueTypeRepository = projectIssueTypeRepository;
        this.projectIssueTypeStatusRepository = projectIssueTypeStatusRepository;
        this.boardRepository = boardRepository;
        this.boardColumnRepository = boardColumnRepository;
    }

    void seed(Project project, User currentUser) {
        List<IssueType> issueTypes = ensureWorkspaceIssueTypes(project, currentUser);
        List<ProjectIssueType> projectIssueTypes = bindIssueTypes(project, issueTypes);
        List<Status> statuses = createStatuses(project, currentUser);
        allowAllCombinations(projectIssueTypes, statuses);
        createDefaultBoard(project, statuses);
    }

    private List<IssueType> ensureWorkspaceIssueTypes(Project project, User currentUser) {
        List<IssueType> existing = issueTypeRepository.findByWorkspace_IdOrderByPosition(project.getWorkspace().getId());
        if (!existing.isEmpty()) {
            return existing;
        }
        List<IssueType> created = new ArrayList<>();
        for (int i = 0; i < DEFAULT_ISSUE_TYPES.size(); i++) {
            IssueType issueType = new IssueType();
            issueType.setWorkspace(project.getWorkspace());
            issueType.setName(DEFAULT_ISSUE_TYPES.get(i));
            issueType.setPosition(i);
            created.add(issueType);
        }
        return issueTypeRepository.saveAll(created);
    }

    private List<ProjectIssueType> bindIssueTypes(Project project, List<IssueType> issueTypes) {
        List<ProjectIssueType> bindings = new ArrayList<>();
        for (int i = 0; i < issueTypes.size(); i++) {
            ProjectIssueType binding = new ProjectIssueType();
            binding.setProject(project);
            binding.setIssueType(issueTypes.get(i));
            binding.setPosition(i);
            bindings.add(binding);
        }
        return projectIssueTypeRepository.saveAll(bindings);
    }

    private List<Status> createStatuses(Project project, User currentUser) {
        List<Status> statuses = new ArrayList<>();
        for (int i = 0; i < DEFAULT_STATUSES.size(); i++) {
            DefaultStatus def = DEFAULT_STATUSES.get(i);
            Status status = new Status();
            status.setProject(project);
            status.setName(def.name());
            status.setCategory(def.category());
            status.setPosition(i);
            status.setCreatedBy(currentUser);
            statuses.add(status);
        }
        return statusRepository.saveAll(statuses);
    }

    private void allowAllCombinations(List<ProjectIssueType> projectIssueTypes, List<Status> statuses) {
        List<ProjectIssueTypeStatus> combinations = new ArrayList<>();
        for (ProjectIssueType type : projectIssueTypes) {
            for (int i = 0; i < statuses.size(); i++) {
                ProjectIssueTypeStatus combination = new ProjectIssueTypeStatus();
                combination.setProjectIssueType(type);
                combination.setStatus(statuses.get(i));
                combination.setPosition(i);
                combinations.add(combination);
            }
        }
        projectIssueTypeStatusRepository.saveAll(combinations);
    }

    private void createDefaultBoard(Project project, List<Status> statuses) {
        Board board = new Board();
        board.setProject(project);
        board.setName("Board");
        board.setDefault(true);
        boardRepository.save(board);

        List<BoardColumn> columns = new ArrayList<>();
        for (int i = 0; i < statuses.size(); i++) {
            BoardColumn column = new BoardColumn();
            column.setBoard(board);
            column.setStatus(statuses.get(i));
            column.setPosition(i);
            columns.add(column);
        }
        boardColumnRepository.saveAll(columns);
    }
}
