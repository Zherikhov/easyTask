package com.easytask.auth.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class InvalidInviteTokenException extends AppException {

    public InvalidInviteTokenException() {
        super(HttpStatus.BAD_REQUEST, "This invite link is invalid or has expired");
    }
}
