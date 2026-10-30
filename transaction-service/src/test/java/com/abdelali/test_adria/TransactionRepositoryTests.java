package com.abdelali.test_adria;

import com.abdelali.test_adria.domain.Transaction;
import com.abdelali.test_adria.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TransactionRepositoryTests {

    @Autowired
    private TransactionRepository repository;

    @Test
    void shouldSaveTransactionWithGeneratedIdAndTimestamp() {
        UUID accountId = UUID.randomUUID();

        Transaction transaction = Transaction.builder()
                .accountId(accountId)
                .amount(new BigDecimal("250.00"))
                .currency("EUR")
                .merchantCategory("SHOPPING")
                .country("FR")
                .build();

        Transaction saved = repository.saveAndFlush(transaction);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAccountId()).isEqualTo(accountId);
        assertThat(saved.getAmount()).isEqualByComparingTo("250.00");
        assertThat(saved.getCreatedAt()).isNotNull();

        Transaction found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getMerchantCategory()).isEqualTo("SHOPPING");
    }

    @Test
    void shouldFindTransactionsByAccountOrderedByDate() {
        UUID accountId = UUID.randomUUID();
        UUID unrelatedAccountId = UUID.randomUUID();

        Transaction older = sample(accountId, "100.00", Instant.parse("2026-10-01T10:00:00Z"));
        Transaction newer = sample(accountId, "200.00", Instant.parse("2026-10-02T10:00:00Z"));
        Transaction unrelated = sample(unrelatedAccountId, "300.00", Instant.parse("2026-10-03T10:00:00Z"));

        repository.saveAllAndFlush(List.of(older, newer, unrelated));

        List<Transaction> history = repository.findByAccountIdOrderByCreatedAtDesc(accountId);

        assertThat(history).hasSize(2);
        assertThat(history).extracting(Transaction::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    private Transaction sample(UUID accountId, String amount, Instant at) {
        return Transaction.builder()
                .accountId(accountId)
                .amount(new BigDecimal(amount))
                .currency("EUR")
                .merchantCategory("SHOPPING")
                .country("FR")
                .createdAt(at)
                .build();
    }
}
