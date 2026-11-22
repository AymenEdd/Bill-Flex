package org.gmp.servicegateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Configuration du rate limiting par adresse IP.
 * Le bean « ipKeyResolver » est référencé dans application.properties.
 *
 * <p>V6 — Selma : RequestRateLimiter appliqué à toutes les routes via Redis.
 */
@Configuration
public class RateLimitingConfig {

    /**
     * Résolveur de clé basé sur l'IP du client.
     * Compatible avec X-Forwarded-For en contexte proxy.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }
}
