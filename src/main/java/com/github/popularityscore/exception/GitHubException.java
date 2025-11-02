package com.github.popularityscore.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class GitHubException extends RuntimeException {
    private final String errorCode;

    public GitHubException(String message) {
        super(message);
        this.errorCode = "GITHUB_ERROR";
    }

    public GitHubException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GITHUB_ERROR";
    }
}
