package com.easytask.project.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class UserNotWorkspaceMemberException extends AppException {

    public UserNotWorkspaceMemberException() {
        super(HttpStatus.BAD_REQUEST, "User must be a workspace member before joining a project");
    }
}
