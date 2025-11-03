package com.github.popularityscore.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class InvalidDateException extends RuntimeException {
    private final String errorCode;

    public InvalidDateException(String message) {
        super(message);
        this.errorCode = "INVALID_DATE_ERROR";
    }

    public InvalidDateException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INVALID_DATE_ERROR";
    }
}
