package com.easytask.workspace.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class InvalidWorkspaceRoleException extends AppException {

    public InvalidWorkspaceRoleException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
