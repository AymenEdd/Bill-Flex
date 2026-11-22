package org.gmp.servicefacturation.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.gmp.servicefacturation.dto.AbonnementDto;
import org.gmp.servicefacturation.dto.ClientDto;
import org.gmp.servicefacturation.dto.PlanDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ExternalServicesClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalServicesClient.class);

    private final AbonnementFeignClient abonnementFeignClient;
    private final ClientServiceClient clientServiceClient;

    @CircuitBreaker(name = "abonnementService", fallbackMethod = "fallbackGetAbonnement")
    @Retry(name = "abonnementService")
    public AbonnementDto getAbonnement(Long subscriptionId) {
        log.info("Appel Feign vers service-abonnements pour subscriptionId={}", subscriptionId);
        return abonnementFeignClient.getAbonnementById(subscriptionId);
    }

    public AbonnementDto fallbackGetAbonnement(Long subscriptionId, Throwable throwable) {
        log.warn("Circuit Breaker / Retry FALLBACK pour subscriptionId={}. Cause: {}", subscriptionId, throwable.getMessage());
        return AbonnementDto.builder()
                .id(subscriptionId)
                .nomPlan("Plan repli (CircuitBreaker)")
                .statut("INDISPONIBLE")
                .build();
    }

    @CircuitBreaker(name = "abonnementService", fallbackMethod = "fallbackGetPlan")
    @Retry(name = "abonnementService")
    public PlanDto getPlan(Long planId) {
        log.info("Appel Feign vers service-abonnements pour planId={}", planId);
        return abonnementFeignClient.getPlanById(planId);
    }

    public PlanDto fallbackGetPlan(Long planId, Throwable throwable) {
        log.warn("Circuit Breaker / Retry FALLBACK pour planId={}. Cause: {}", planId, throwable.getMessage());
        return PlanDto.builder()
                .id(planId)
                .nom("Plan de repli")
                .prixMensuel(BigDecimal.ZERO)
                .quotaUtilisateurs(0)
                .actif(false)
                .build();
    }

    @CircuitBreaker(name = "clientService", fallbackMethod = "fallbackGetClient")
    @Retry(name = "clientService")
    public ClientDto getClient(Long clientId) {
        log.info("Appel Feign vers service-clients pour clientId={}", clientId);
        return clientServiceClient.getClientById(clientId);
    }

    public ClientDto fallbackGetClient(Long clientId, Throwable throwable) {
        log.warn("Circuit Breaker / Retry FALLBACK pour clientId={}. Cause: {}", clientId, throwable.getMessage());
        return ClientDto.builder()
                .id(clientId)
                .nom("Client Repli (CircuitBreaker)")
                .email("repli@billflex.local")
                .numeroTVA(null)
                .modePaiement(null)
                .statut("INDISPONIBLE")
                .build();
    }
}
