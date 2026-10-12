package com.tikahome.exception;

/** Wrong email or password. Deliberately doesn't say which one. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
