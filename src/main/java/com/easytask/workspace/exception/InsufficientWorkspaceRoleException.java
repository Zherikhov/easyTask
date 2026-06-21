package com.easytask.workspace.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class InsufficientWorkspaceRoleException extends AppException {

    public InsufficientWorkspaceRoleException() {
        super(HttpStatus.FORBIDDEN, "Only workspace owners or admins can perform this action");
    }
}
