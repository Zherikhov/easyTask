package com.easytask.project.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class ProjectAccessDeniedException extends AppException {

    public ProjectAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "You don't have permission to perform this action in this project");
    }
}
