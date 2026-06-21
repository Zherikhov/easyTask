package com.easytask.project.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class ProjectNotFoundException extends AppException {

    public ProjectNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Project not found");
    }
}
