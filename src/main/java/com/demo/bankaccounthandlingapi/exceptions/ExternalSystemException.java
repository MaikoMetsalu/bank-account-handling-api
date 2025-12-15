package com.demo.bankaccounthandlingapi.exceptions;

public class ExternalSystemException extends RuntimeException {
    public ExternalSystemException(String message) {
        super(message);
    }
}
