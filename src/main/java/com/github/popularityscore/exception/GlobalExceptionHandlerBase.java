package com.github.popularityscore.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Base global exception handler for REST APIs.
 * Provides generic handlers for Spring MVC and GitHub exceptions.
 */
@Slf4j
@Generated
public abstract class GlobalExceptionHandlerBase {

    // ---------------------------
    // 🔹 Common Framework Exceptions
    // ---------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleHttpRequestMethodNotSupportedException(
            HttpServletRequest req, HttpRequestMethodNotSupportedException ex) {

        log.warn("Method Not Allowed [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported", ex.getMessage(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(
            HttpServletRequest req, HttpMessageNotReadableException ex) {

        log.warn("Malformed JSON request [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", ex.getMessage(), req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameterException(
            HttpServletRequest req, MissingServletRequestParameterException ex) {

        log.warn("Missing request parameter [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Missing parameter", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(
            HttpServletRequest req, MethodArgumentNotValidException ex) {

        log.warn("Validation failed [{} {}]", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", ex.getMessage(), req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
            HttpServletRequest req, ConstraintViolationException ex) {

        log.warn("Constraint violation [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Constraint violation", ex.getMessage(), req);
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleMultipartException(
            HttpServletRequest req, MultipartException ex) {

        log.warn("Multipart error [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "File upload error", ex.getMessage(), req);
    }

    // ---------------------------
    // 🔹 Generic & fallback
    // ---------------------------

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleThrowable(HttpServletRequest req, Throwable ex) {
        log.error("Unhandled exception [{} {}]", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", ex.getMessage(), req);
    }

    // ---------------------------
    // 🧩 Helper to build uniform response
    // ---------------------------

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message, HttpServletRequest req) {
        return build(status, error, message, req, null);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message,
                                                      HttpServletRequest req, Map<String, Object> details) {

        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", req.getRequestURI());
        if (details != null && !details.isEmpty()) {
            body.put("details", details);
        }

        return ResponseEntity.status(status).body(body);
    }
}
