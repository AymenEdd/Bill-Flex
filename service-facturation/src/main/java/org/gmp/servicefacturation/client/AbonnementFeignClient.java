package org.gmp.servicefacturation.client;

import org.gmp.servicefacturation.dto.AbonnementDto;
import org.gmp.servicefacturation.dto.PlanDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "service-abonnements",
        url = "${service-abonnements.url:http://localhost:8081}",
        fallback = AbonnementClientFallback.class
)
public interface AbonnementFeignClient {

    @GetMapping("/api/abonnements/{id}")
    AbonnementDto getAbonnementById(@PathVariable("id") Long id);

    @GetMapping("/api/plans/{id}")
    PlanDto getPlanById(@PathVariable("id") Long id);
}
