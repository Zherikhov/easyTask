package com.easytask.auth.exception;
import com.easytask.common.exception.AppException;

import org.springframework.http.HttpStatus;

public class RegistrationDisabledException extends AppException {

    public RegistrationDisabledException() {
        super(HttpStatus.FORBIDDEN, "Public self-registration is disabled on this instance");
    }
}
