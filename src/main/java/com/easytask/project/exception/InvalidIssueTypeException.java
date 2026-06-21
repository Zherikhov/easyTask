package com.easytask.project.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class InvalidIssueTypeException extends AppException {

    public InvalidIssueTypeException() {
        super(HttpStatus.BAD_REQUEST, "Issue type is not enabled for this project");
    }
}
