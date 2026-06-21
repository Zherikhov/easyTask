package com.easytask.project.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class InsufficientProjectRoleException extends AppException {

    public InsufficientProjectRoleException() {
        super(HttpStatus.FORBIDDEN, "Only a project lead or workspace admin/owner can do this");
    }
}
