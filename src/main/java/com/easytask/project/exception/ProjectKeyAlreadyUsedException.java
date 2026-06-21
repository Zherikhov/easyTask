package com.easytask.project.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class ProjectKeyAlreadyUsedException extends AppException {

    public ProjectKeyAlreadyUsedException(String key) {
        super(HttpStatus.CONFLICT, "Project key already in use in this workspace: " + key);
    }
}
