package com.easytask.issue.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class CommentAccessDeniedException extends AppException {

    public CommentAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "Only the comment's author can do this");
    }
}
