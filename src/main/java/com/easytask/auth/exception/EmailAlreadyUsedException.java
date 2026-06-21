package com.easytask.auth.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class EmailAlreadyUsedException extends AppException {

    public EmailAlreadyUsedException(String email) {
        super(HttpStatus.CONFLICT, "Email already in use: " + email);
    }
}
