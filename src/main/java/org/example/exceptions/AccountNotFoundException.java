package org.example.exceptions;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long id) {
        System.out.println("AccountNotFoundException: user " + id);
    }
}
