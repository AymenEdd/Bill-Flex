package org.gmp.serviceabonnements.mapper;

import org.gmp.serviceabonnements.dto.PlanRequest;
import org.gmp.serviceabonnements.dto.PlanResponse;
import org.gmp.serviceabonnements.entity.Plan;

public class PlanMapper {

    private PlanMapper() {
    }

    public static Plan toEntity(PlanRequest request) {
        Plan plan = new Plan();

        plan.setNom(request.getNom());
        plan.setPrixMensuel(request.getPrixMensuel());
        plan.setQuotaUtilisateurs(request.getQuotaUtilisateurs());
        plan.setActif(true);

        return plan;
    }

    public static void updateEntity(Plan plan, PlanRequest request) {
        plan.setNom(request.getNom());
        plan.setPrixMensuel(request.getPrixMensuel());
        plan.setQuotaUtilisateurs(request.getQuotaUtilisateurs());
    }

    public static PlanResponse toResponse(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getNom(),
                plan.getPrixMensuel(),
                plan.getQuotaUtilisateurs(),
                plan.getActif()
        );
    }
}
