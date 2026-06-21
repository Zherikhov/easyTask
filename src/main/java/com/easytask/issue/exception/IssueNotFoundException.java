package com.easytask.issue.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class IssueNotFoundException extends AppException {

    public IssueNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Issue not found");
    }
}
