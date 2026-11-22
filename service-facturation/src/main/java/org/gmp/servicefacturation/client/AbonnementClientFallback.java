package org.gmp.servicefacturation.client;

import org.gmp.servicefacturation.dto.AbonnementDto;
import org.gmp.servicefacturation.dto.PlanDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AbonnementClientFallback implements AbonnementFeignClient {

    private static final Logger log = LoggerFactory.getLogger(AbonnementClientFallback.class);

    @Override
    public AbonnementDto getAbonnementById(Long id) {
        log.warn("Fallback déclenché pour getAbonnementById({}) : service-abonnements indisponible", id);
        return AbonnementDto.builder()
                .id(id)
                .nomPlan("Plan indisponible (Fallback)")
                .statut("INDISPONIBLE")
                .build();
    }

    @Override
    public PlanDto getPlanById(Long id) {
        log.warn("Fallback déclenché pour getPlanById({}) : service-abonnements indisponible", id);
        return PlanDto.builder()
                .id(id)
                .nom("Plan de secours (Fallback)")
                .prixMensuel(BigDecimal.ZERO)
                .quotaUtilisateurs(0)
                .actif(false)
                .build();
    }
}
