package com.easytask.common.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends AppException {

    public UserNotFoundException(String email) {
        super(HttpStatus.NOT_FOUND, "No user found with email: " + email);
    }
}
