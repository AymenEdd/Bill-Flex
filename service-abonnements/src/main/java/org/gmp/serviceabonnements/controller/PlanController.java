package org.gmp.serviceabonnements.controller;

import jakarta.validation.Valid;
import org.gmp.serviceabonnements.dto.PlanRequest;
import org.gmp.serviceabonnements.dto.PlanResponse;
import org.gmp.serviceabonnements.service.PlanService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    // Create a plan
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResponse create(@Valid @RequestBody PlanRequest request) {
        return planService.create(request);
    }

    // Get all plans
    @GetMapping
    public List<PlanResponse> findAll() {
        return planService.findAll();
    }

    // Get active plans
    @GetMapping("/active")
    public List<PlanResponse> findActive() {
        return planService.findActive();
    }

    // Get plan by ID
    @GetMapping("/{id}")
    public PlanResponse findById(@PathVariable Long id) {
        return planService.findById(id);
    }

    // Update a plan
    @PutMapping("/{id}")
    public PlanResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PlanRequest request) {
        return planService.update(id, request);
    }

    // Deactivate a plan
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        planService.deactivate(id);
    }
}

