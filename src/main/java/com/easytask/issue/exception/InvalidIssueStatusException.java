package com.easytask.issue.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class InvalidIssueStatusException extends AppException {

    public InvalidIssueStatusException() {
        super(HttpStatus.BAD_REQUEST, "Status is not allowed for this issue's type");
    }
}
