package com.easytask.issue.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class CommentNotFoundException extends AppException {

    public CommentNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Comment not found");
    }
}
