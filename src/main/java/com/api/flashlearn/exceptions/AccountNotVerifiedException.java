package com.api.flashlearn.exceptions;

public class AccountNotVerifiedException extends RuntimeException {
    public AccountNotVerifiedException() {
        super("Account not verified. Please verify your email.");
    }
    public AccountNotVerifiedException(String message) {
        super(message);
    }
    
}
