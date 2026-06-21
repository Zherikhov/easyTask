package com.easytask.workspace.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class AlreadyWorkspaceMemberException extends AppException {

    public AlreadyWorkspaceMemberException() {
        super(HttpStatus.CONFLICT, "User is already a member of this workspace");
    }
}
