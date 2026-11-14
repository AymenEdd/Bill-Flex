package org.gmp.serviceabonnements.service;

import org.gmp.serviceabonnements.dto.PlanRequest;
import org.gmp.serviceabonnements.dto.PlanResponse;
import org.gmp.serviceabonnements.entity.Plan;
import org.gmp.serviceabonnements.mapper.PlanMapper;
import org.gmp.serviceabonnements.repository.PlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    // Create a new plan
    public PlanResponse create(PlanRequest request) {

        if (planRepository.existsByNom(request.getNom())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un plan avec ce nom existe déjà"
            );
        }

        Plan plan = PlanMapper.toEntity(request);

        return PlanMapper.toResponse(
                planRepository.save(plan)
        );
    }

    // Get all plans
    public List<PlanResponse> findAll() {
        return planRepository.findAll()
                .stream()
                .map(PlanMapper::toResponse)
                .toList();
    }

    // Get active plans
    public List<PlanResponse> findActive() {
        return planRepository.findByActifTrue()
                .stream()
                .map(PlanMapper::toResponse)
                .toList();
    }

    // Get plan by ID
    public PlanResponse findById(Long id) {
        Plan plan = getPlanOrThrow(id);
        return PlanMapper.toResponse(plan);
    }

    // Update a plan
    public PlanResponse update(Long id, PlanRequest request) {

        Plan plan = getPlanOrThrow(id);

        if (planRepository.existsByNom(request.getNom())
                && !plan.getNom().equals(request.getNom())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un plan avec ce nom existe déjà"
            );
        }

        PlanMapper.updateEntity(plan, request);

        return PlanMapper.toResponse(
                planRepository.save(plan)
        );
    }

    // Soft delete: deactivate a plan
    public void deactivate(Long id) {
        Plan plan = getPlanOrThrow(id);
        plan.setActif(false);
        planRepository.save(plan);
    }

    private Plan getPlanOrThrow(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Plan introuvable avec ID : " + id
                ));
    }
}

