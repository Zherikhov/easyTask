package com.easytask.project.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class UserNotProjectMemberException extends AppException {

    public UserNotProjectMemberException() {
        super(HttpStatus.BAD_REQUEST, "Assignee must be a member of this project");
    }
}
