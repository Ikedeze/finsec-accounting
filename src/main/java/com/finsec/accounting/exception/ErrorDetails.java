package com.finsec.accounting.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public class ErrorDetails {
    private String message;
    private int status;
    private Instant timestamp;
    private String error;
    private String path;

    public ErrorDetails() {}

    public ErrorDetails(HttpStatus httpStatus, String error, String path) {
        this.message = message;
        this.status = httpStatus.value();
        this.timestamp = timestamp.now();
        this.error = httpStatus.getReasonPhrase();
        this.path = path;
    }

    public String getMessage() {return message;}
    public int getStatus() {return status;}
    public Instant getTimestamp() {return timestamp;}
    public String getError() {return error;}
    public String getPath() {return path;}
}
