package com.easytask.project.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class InvalidProjectKeyException extends AppException {

    public InvalidProjectKeyException() {
        super(HttpStatus.BAD_REQUEST,
                "Project key must start with a letter and contain 2-10 uppercase letters/digits");
    }
}
