package com.ben.services.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserIdentityConflictException extends RuntimeException {

    public UserIdentityConflictException(String message) {
        super(message);
    }
}
