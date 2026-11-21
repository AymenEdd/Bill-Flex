package org.gmp.service_notifications.client;

import java.time.Duration;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.gmp.service_notifications.entity.NotificationFailure;
import org.springframework.stereotype.Service;

@Service
public class CustomerLookup {

    private final CustomerClient client;
    private final CircuitBreaker breaker = CircuitBreaker.of("service-clients",
            CircuitBreakerConfig.custom()
                    .slidingWindowSize(4)
                    .minimumNumberOfCalls(4)
                    .permittedNumberOfCallsInHalfOpenState(1)
                    .waitDurationInOpenState(Duration.ofSeconds(15))
                    .ignoreExceptions(Rejected.class)
                    .build());
    private final Retry retry = Retry.of("service-clients",
            RetryConfig.custom()
                    .maxAttempts(3)
                    .waitDuration(Duration.ofMillis(150))
                    .retryExceptions(Unavailable.class)
                    .build());

    public CustomerLookup(CustomerClient client) {
        this.client = client;
    }

    public String fetchEmail(Long clientId) {
        try {
            return breaker.executeSupplier(() -> retry.executeSupplier(() -> lookup(clientId)));
        } catch (CallNotPermittedException ex) {
            throw new Unavailable(ex);
        }
    }

    private String lookup(Long clientId) {
        CustomerClient.CustomerResponse customer;
        try {
            customer = client.findById(clientId);
        } catch (FeignException ex) {
            int status = ex.status();
            if (status == 404) {
                throw new Rejected(NotificationFailure.CLIENT_INTROUVABLE);
            }
            if (status < 0 || status == 429 || status >= 500) {
                throw new Unavailable(ex);
            }
            throw new Rejected(NotificationFailure.REPONSE_CLIENT_INVALIDE);
        }

        if (customer == null || !clientId.equals(customer.id())) {
            throw new Rejected(NotificationFailure.REPONSE_CLIENT_INVALIDE);
        }
        return validateEmail(customer.email());
    }

    private String validateEmail(String email) {
        if (email == null || email.isBlank() || email.length() > 255
                || email.indexOf('\r') >= 0 || email.indexOf('\n') >= 0) {
            throw new Rejected(NotificationFailure.EMAIL_INVALIDE);
        }

        try {
            InternetAddress address = new InternetAddress(email, true);
            address.validate();
            if (address.isGroup() || !email.equals(address.getAddress())) {
                throw new Rejected(NotificationFailure.EMAIL_INVALIDE);
            }
            return email;
        } catch (AddressException ex) {
            throw new Rejected(NotificationFailure.EMAIL_INVALIDE);
        }
    }

    public static class Unavailable extends RuntimeException {
        Unavailable(RuntimeException cause) {
            super("Service clients temporairement indisponible", cause);
        }
    }

    public static class Rejected extends RuntimeException {
        private final NotificationFailure failure;

        Rejected(NotificationFailure failure) {
            super(failure.name());
            this.failure = failure;
        }

        public NotificationFailure getFailure() {
            return failure;
        }
    }
}
