
package org.gmp.serviceabonnements.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
    public ClientDto getClientById(Long id) {
        return clientServiceClient.getClientById(id);
    }

    private ClientDto getClientByIdFallback(Long id, Throwable ex) {
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
    public List<ClientDto> getAllClients() {
        return clientServiceClient.getAllClients();
    }

    private List<ClientDto> getAllClientsFallback(Throwable ex) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to retrieve clients because the client service is unavailable."
        );
    }
}
