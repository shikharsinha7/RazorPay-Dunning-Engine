package com.buildathon.dunning.seed;

import com.buildathon.dunning.entity.Customer;
import com.buildathon.dunning.entity.FailureEvent;
import com.buildathon.dunning.entity.Payment;
import com.buildathon.dunning.entity.enums.FailureCode;
import com.buildathon.dunning.entity.enums.PaymentStatus;
import com.buildathon.dunning.repository.CustomerRepository;
import com.buildathon.dunning.repository.FailureEventRepository;
import com.buildathon.dunning.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Seeds the database with realistic failed-payment data on first startup so
 * the rest of the build (retry scheduler, AI classification, dashboard) has
 * something real to work against. Skips seeding if payments already exist,
 * so it's safe to restart the app repeatedly.
 */
@Component
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);

    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final FailureEventRepository failureEventRepository;

    private static final int NUM_CUSTOMERS = 60;
    private static final int MIN_PAYMENTS_PER_CUSTOMER = 1;
    private static final int MAX_PAYMENTS_PER_CUSTOMER = 4;

    // Weighted so INSUFFICIENT_FUNDS and BANK_DECLINE dominate, mirroring
    // real-world dunning failure-code distributions.
    private static final List<FailureCode> WEIGHTED_FAILURE_CODES = List.of(
            FailureCode.INSUFFICIENT_FUNDS, FailureCode.INSUFFICIENT_FUNDS,
            FailureCode.INSUFFICIENT_FUNDS, FailureCode.INSUFFICIENT_FUNDS,
            FailureCode.BANK_DECLINE, FailureCode.BANK_DECLINE, FailureCode.BANK_DECLINE,
            FailureCode.EXPIRED_CARD, FailureCode.EXPIRED_CARD,
            FailureCode.GATEWAY_TIMEOUT, FailureCode.GATEWAY_TIMEOUT
    );

    private static final String[] FIRST_NAMES = {
            "Aarav", "Vivaan", "Aditi", "Ananya", "Ishaan", "Diya", "Kabir", "Meera", "Rohan", "Sanya",
            "Arjun", "Priya", "Karan", "Neha", "Yash", "Riya", "Dev", "Tara", "Vikram", "Anika"
    };
    private static final String[] LAST_NAMES = {
            "Sharma", "Verma", "Gupta", "Iyer", "Reddy", "Nair", "Menon", "Kapoor", "Rao", "Chatterjee"
    };

    // Fixed seed so the demo dataset is reproducible across restarts/redeploys
    private final Random random = new Random(42);

    @Override
    public void run(String... args) {
        if (paymentRepository.count() > 0) {
            log.info("Seed data already present ({} payments) — skipping seeding.", paymentRepository.count());
            return;
        }

        log.info("Seeding customers and failed payments...");

        List<Customer> customers = customerRepository.saveAll(generateCustomers());
        int totalPayments = 0;

        for (Customer customer : customers) {
            int paymentCount = MIN_PAYMENTS_PER_CUSTOMER
                    + random.nextInt(MAX_PAYMENTS_PER_CUSTOMER - MIN_PAYMENTS_PER_CUSTOMER + 1);
            for (int i = 0; i < paymentCount; i++) {
                Payment payment = paymentRepository.save(createFailedPayment(customer));
                failureEventRepository.save(createFailureEvent(payment));
                totalPayments++;
            }
        }

        log.info("Seeded {} customers and {} failed payments.", customers.size(), totalPayments);
    }

    private List<Customer> generateCustomers() {
        return IntStream.range(0, NUM_CUSTOMERS)
                .mapToObj(i -> {
                    String first = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                    String last = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                    String email = (first + "." + last + i).toLowerCase() + "@example.com";
                    return Customer.builder()
                            .name(first + " " + last)
                            .email(email)
                            .createdAt(randomPastInstant(180))
                            .build();
                })
                .toList();
    }

    private Payment createFailedPayment(Customer customer) {
        BigDecimal amount = BigDecimal.valueOf(500 + random.nextInt(49_500))
                .setScale(2, RoundingMode.HALF_UP);
        return Payment.builder()
                .customer(customer)
                .amount(amount)
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .createdAt(randomPastInstant(14))
                .build();
    }

    private FailureEvent createFailureEvent(Payment payment) {
        FailureCode code = WEIGHTED_FAILURE_CODES.get(random.nextInt(WEIGHTED_FAILURE_CODES.size()));
        return FailureEvent.builder()
                .payment(payment)
                .failureCode(code)
                .occurredAt(payment.getCreatedAt().plus(random.nextInt(3600), ChronoUnit.SECONDS))
                .build();
    }

    private Instant randomPastInstant(int maxDaysAgo) {
        long secondsAgo = (long) random.nextInt(maxDaysAgo * 24 * 60 * 60);
        return Instant.now().minusSeconds(secondsAgo);
    }
}
