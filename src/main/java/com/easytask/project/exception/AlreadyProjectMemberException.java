package com.easytask.project.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class AlreadyProjectMemberException extends AppException {

    public AlreadyProjectMemberException() {
        super(HttpStatus.CONFLICT, "User is already a member of this project");
    }
}
