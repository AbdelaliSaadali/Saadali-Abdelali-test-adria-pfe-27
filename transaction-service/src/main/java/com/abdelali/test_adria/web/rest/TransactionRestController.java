package com.abdelali.test_adria.web.rest;

import com.abdelali.test_adria.service.TransactionService;
import com.abdelali.test_adria.service.dto.CreateTransactionRequest;
import com.abdelali.test_adria.service.dto.CreateTransferRequest;
import com.abdelali.test_adria.service.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionRestController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.create(request));
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransactionResponse> transfer(
            @RequestBody CreateTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(request));
    }

    @GetMapping("/{id}")
    public TransactionResponse byId(@PathVariable UUID id) {
        return transactionService.getById(id);
    }

    @GetMapping("/account/{accountId}")
    public List<TransactionResponse> history(@PathVariable UUID accountId) {
        return transactionService.history(accountId);
    }
}
