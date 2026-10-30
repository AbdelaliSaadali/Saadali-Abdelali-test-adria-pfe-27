package com.abdelali.test_adria.web.rest;

import com.abdelali.test_adria.service.AccountDependencyException;
import com.abdelali.test_adria.service.TransactionBusinessException;
import com.abdelali.test_adria.service.TransactionNotFoundException;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class TransactionRestExceptionHandler {

    @ExceptionHandler(TransactionBusinessException.class)
    public ResponseEntity<Map<String, String>> badRequest(TransactionBusinessException error) {
        return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(TransactionNotFoundException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(AccountDependencyException.class)
    public ResponseEntity<Map<String, String>> unavailable(AccountDependencyException error) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, String>> remoteError(FeignException error) {
        if (error.status() == 404) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Account not found"));
        }
        if (error.status() == 400) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Account-Service rejected the operation"));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Account-Service request failed"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> malformedRequest(Exception error) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid request payload"));
    }
}
