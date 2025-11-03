package com.github.popularityscore.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends GlobalExceptionHandlerBase {

    @ExceptionHandler(GitHubException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public @ResponseBody ExceptionResponse handleGitHubException(GitHubException ex, HttpServletRequest req) {
        System.out.println("Caught GitHubException"+ ex);
        return getExceptionResponse(ex, req);
    }

    @ExceptionHandler(InvalidDateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ExceptionResponse handleInvalidDate(
            InvalidDateException ex, HttpServletRequest req) {

        System.out.println("Caught InvalidDateException"+ ex);
        return getExceptionResponse(ex, req);
    }

    private ExceptionResponse getExceptionResponse(Exception ex, HttpServletRequest req) {
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setErrorMessage(ex.getMessage());
        if(ex.getCause() != null) {
            exceptionResponse.setCause(ex.getCause().toString());
        }
        exceptionResponse.setRequestedURI(req.getRequestURI());
        return exceptionResponse;
    }
}
