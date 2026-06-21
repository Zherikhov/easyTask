package com.easytask.workspace.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class WorkspaceNotFoundException extends AppException {

    public WorkspaceNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Workspace not found");
    }
}
