package com.abdelali.test_adria.service;

public class TransactionBusinessException extends RuntimeException {
    public TransactionBusinessException(String message) {
        super(message);
    }
}
