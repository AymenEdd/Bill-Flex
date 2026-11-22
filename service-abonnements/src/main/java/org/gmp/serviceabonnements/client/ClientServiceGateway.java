
package org.gmp.serviceabonnements.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.gmp.serviceabonnements.chaos.ChaosModeContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
public class ClientServiceGateway {

    private final ClientServiceClient clientServiceClient;

    public ClientServiceGateway(ClientServiceClient clientServiceClient) {
        this.clientServiceClient = clientServiceClient;
    }

    @CircuitBreaker(
            name = "clientById",
            fallbackMethod = "getClientByIdFallback"
    )
    @Retry(name = "clientById")
    @TimeLimiter(name = "clientById")
    public CompletableFuture<ClientDto> getClientById(Long id) {
        return withChaos(() -> clientServiceClient.getClientById(id));
    }

    private CompletableFuture<ClientDto> getClientByIdFallback(Long id, Throwable ex) {
        if (ex instanceof feign.FeignException fe && fe.status() == 404) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        if (ex.getCause() instanceof feign.FeignException fe && fe.status() == 404) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        if (ex instanceof ResponseStatusException rse) {
            throw rse;
        }
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Client service is unavailable. Please try again later."
        );
    }

    @CircuitBreaker(
            name = "clientsList",
            fallbackMethod = "getAllClientsFallback"
    )
    @Retry(name = "clientsList")
    @TimeLimiter(name = "clientsList")
    public CompletableFuture<List<ClientDto>> getAllClients() {
        return withChaos(clientServiceClient::getAllClients);
    }

    private CompletableFuture<List<ClientDto>> getAllClientsFallback(Throwable ex) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to retrieve clients because the client service is unavailable."
        );
    }

    /**
     * Wraps the supplier in a {@link CompletableFuture} that simulates network behaviour
     * depending on the active Chaos-Mode:
     * <ul>
     *   <li>{@code delay}   → result after 2s (within the 3s TimeLimiter → success)</li>
     *   <li>{@code timeout} → result after 5s (exceeds the 3s TimeLimiter → fallback 503)</li>
     * </ul>
     * The {@code fail} mode is handled directly by {@link ChaosModeFilter} (503 before the gateway is reached).
     */
    private <T> CompletableFuture<T> withChaos(Supplier<T> supplier) {
        String mode = ChaosModeContext.get();
        if (mode == null) {
            return CompletableFuture.completedFuture(supplier.get());
        }
        switch (mode.toLowerCase()) {
            case "delay":
                return CompletableFuture.supplyAsync(() -> {
                    sleep(2000); // within 3s TimeLimiter → succeeds
                    return supplier.get();
                });
            case "timeout":
                return CompletableFuture.supplyAsync(() -> {
                    sleep(5000); // exceeds 3s TimeLimiter → triggers TimeLimiter
                    return supplier.get();
                });
            default:
                return CompletableFuture.completedFuture(supplier.get());
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
