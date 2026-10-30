package com.abdelali.test_adria.web.rest;

import com.abdelali.test_adria.domain.AccountStatus;
import com.abdelali.test_adria.service.AccountService;
import com.abdelali.test_adria.service.dto.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountRestController {

    private final AccountService accountService;

    @GetMapping
    public List<AccountResponse> getAllAccounts() {
        return accountService.findAll();
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable UUID id) {
        return accountService.findById(id);
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.create(request));
    }

    @PatchMapping("/{id}/status")
    public AccountResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusRequest request) {

        return accountService.changeStatus(id, request.status());
    }

    @PostMapping("/{id}/debit")
    public AccountResponse debit(
            @PathVariable UUID id,
            @Valid @RequestBody AmountRequest request) {

        return accountService.debit(id, request.amount());
    }

    @PostMapping("/{id}/credit")
    public AccountResponse credit(
            @PathVariable UUID id,
            @Valid @RequestBody AmountRequest request) {

        return accountService.credit(id, request.amount());
    }

    @PostMapping("/transfers")
    public TransferResponse transfer(
            @Valid @RequestBody TransferRequest request) {

        return accountService.transfer(
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount()
        );
    }
}