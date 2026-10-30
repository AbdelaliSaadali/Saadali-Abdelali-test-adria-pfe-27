package com.abdelali.test_adria.web.rest;

import com.abdelali.test_adria.service.*;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class AccountRestExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(
            AccountNotFoundException exception) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler({
            AccountBusinessException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, String>> badRequest(
            RuntimeException exception) {

        return ResponseEntity.badRequest()
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validationError(
            MethodArgumentNotValidException exception) {

        return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid request data"));
    }

    @ExceptionHandler({
            OptimisticLockingFailureException.class,
            PessimisticLockingFailureException.class
    })
    public ResponseEntity<Map<String, String>> concurrencyError(
            RuntimeException exception) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error",
                        "Concurrent account update; please retry"
                ));
    }
}