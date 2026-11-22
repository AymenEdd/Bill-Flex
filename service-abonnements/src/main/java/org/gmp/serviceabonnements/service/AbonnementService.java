package org.gmp.serviceabonnements.service;

import org.gmp.serviceabonnements.client.ClientServiceGateway;
import org.gmp.serviceabonnements.dto.AbonnementRequest;
import org.gmp.serviceabonnements.dto.AbonnementResponse;
import org.gmp.serviceabonnements.entity.Abonnement;
import org.gmp.serviceabonnements.entity.Plan;
import org.gmp.serviceabonnements.entity.StatutAbonnement;
import org.gmp.serviceabonnements.mapper.AbonnementMapper;
import org.gmp.serviceabonnements.repository.AbonnementRepository;
import org.gmp.serviceabonnements.repository.PlanRepository;
import org.gmp.serviceabonnements.dto.AbonnementStatisticsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.gmp.serviceabonnements.dto.AbonnementUpdateRequest;
import org.gmp.serviceabonnements.client.ClientDto;
import org.gmp.serviceabonnements.client.ClientServiceClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.gmp.serviceabonnements.dto.AbonnementNotificationResponse;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.gmp.serviceabonnements.dto.AbonnementDetailsResponse;
import org.gmp.serviceabonnements.dto.PlanDetailsResponse;
@Service
@Transactional
public class AbonnementService {
    private final AbonnementEmailService abonnementEmailService;
    private final AbonnementRepository abonnementRepository;
    private final PlanRepository planRepository;
    private final ClientServiceGateway clientServiceGateway;
    public AbonnementService(
            AbonnementRepository abonnementRepository,
            PlanRepository planRepository,
            ClientServiceGateway clientServiceGateway,
            AbonnementEmailService abonnementEmailService
    ) {
        this.abonnementRepository = abonnementRepository;
        this.planRepository = planRepository;
        this.clientServiceGateway = clientServiceGateway;
        this.abonnementEmailService = abonnementEmailService;
    }

    // Create a subscription
    public AbonnementResponse create(
            AbonnementRequest request,
            String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key header is required"
            );
        }

        if (idempotencyKey.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must not exceed 100 characters"
            );
        }

        var existing = abonnementRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            return AbonnementMapper.toResponse(existing.get());
        }
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Plan not found"
                ));

        if (!Boolean.TRUE.equals(plan.getActif())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This plan is inactive"
            );
        }

        if (request.getDateFin() != null &&
                request.getDateFin().isBefore(request.getDateDebut())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End date cannot be before start date"
            );
        }

        boolean alreadySubscribed =
                abonnementRepository.existsByClientIdAndStatut(
                        request.getClientId(),
                        StatutAbonnement.ACTIF
                );

        if (alreadySubscribed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Client already has an active subscription"
            );
        }
        // Verify that the client exists
        ClientDto client = clientServiceGateway.getClientById(
                request.getClientId()
        );

        if (client == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Client not found"
            );
        }

// Verify that the client is active
        if (client.getStatut() == null ||
                !client.getStatut().equalsIgnoreCase("Actif")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Client is not active"
            );
        }
        Abonnement abonnement =
                AbonnementMapper.toEntity(request, plan);
        abonnement.setIdempotencyKey(idempotencyKey);
        Abonnement saved =
                abonnementRepository.save(abonnement);

        return AbonnementMapper.toResponse(saved);
    }

    // Get all subscriptions
    @Transactional(readOnly = true)
    public List<AbonnementResponse> findAll() {
        return abonnementRepository.findAll()
                .stream()
                .map(AbonnementMapper::toResponse)
                .toList();
    }

    // Get subscriptions by client
    @Transactional(readOnly = true)
    public List<AbonnementResponse> findByClientId(Long clientId) {
        return abonnementRepository.findByClientId(clientId)
                .stream()
                .map(AbonnementMapper::toResponse)
                .toList();
    }

    // Get subscription by ID
    @Transactional(readOnly = true)
    public AbonnementResponse findById(Long id) {

        Abonnement abonnement = abonnementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subscription not found"
                ));

        return AbonnementMapper.toResponse(abonnement);
    }

    // Suspend a subscription
    public AbonnementResponse suspend(Long id) {
        Abonnement abonnement = getAbonnementOrThrow(id);

        if (abonnement.getStatut() != StatutAbonnement.ACTIF) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only active subscriptions can be suspended"
            );
        }

        abonnement.setStatut(StatutAbonnement.SUSPENDU);

        return AbonnementMapper.toResponse(
                abonnementRepository.save(abonnement)
        );
    }

    // Reactivate a subscription
    public AbonnementResponse reactivate(Long id) {
        Abonnement abonnement = getAbonnementOrThrow(id);

        if (abonnement.getStatut() != StatutAbonnement.SUSPENDU) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only suspended subscriptions can be reactivated"
            );
        }

        abonnement.setStatut(StatutAbonnement.ACTIF);

        return AbonnementMapper.toResponse(
                abonnementRepository.save(abonnement)
        );
    }

    // Cancel a subscription
    public AbonnementResponse cancel(Long id) {
        Abonnement abonnement = getAbonnementOrThrow(id);

        if (abonnement.getStatut() == StatutAbonnement.RESILIE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Subscription is already cancelled"
            );
        }

        abonnement.setStatut(StatutAbonnement.RESILIE);
        abonnement.setDateFin(java.time.LocalDate.now());

        return AbonnementMapper.toResponse(
                abonnementRepository.save(abonnement)
        );
    }

    // Find subscription or return 404
    private Abonnement getAbonnementOrThrow(Long id) {
        return abonnementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subscription not found"
                ));
    }

    // Expire subscriptions whose end date has been reached
    public int expireSubscriptions() {

        List<Abonnement> expiredSubscriptions =
                abonnementRepository.findByStatutAndDateFinLessThanEqual(
                        StatutAbonnement.ACTIF,
                        LocalDate.now()
                );

        for (Abonnement abonnement : expiredSubscriptions) {
            abonnement.setStatut(StatutAbonnement.EXPIRE);
        }

        abonnementRepository.saveAll(expiredSubscriptions);

        return expiredSubscriptions.size();
    }
    // Process subscriptions whose billing date has arrived
    public int processDueBillingDates() {

        LocalDate today = LocalDate.now();

        List<Abonnement> dueSubscriptions =
                abonnementRepository
                        .findByStatutAndProchaineDateFacturationLessThanEqual(
                                StatutAbonnement.ACTIF,
                                today
                        );

        int processed = 0;

        for (Abonnement abonnement : dueSubscriptions) {

            // Do not renew subscriptions that have reached their end date
            if (abonnement.getDateFin() != null &&
                    !abonnement.getDateFin().isAfter(today)) {

                abonnement.setStatut(StatutAbonnement.EXPIRE);
                continue;
            }

            LocalDate nextDate =
                    abonnement.getProchaineDateFacturation();

            // Advance to the next future billing date
            do {
                nextDate = nextDate.plusMonths(1);
            } while (!nextDate.isAfter(today));

            abonnement.setProchaineDateFacturation(nextDate);
            processed++;
        }

        abonnementRepository.saveAll(dueSubscriptions);

        return processed;
    }
    // Get subscriptions with pagination
    @Transactional(readOnly = true)
    public Page<AbonnementResponse> findAllPaginated(Pageable pageable) {

        return abonnementRepository.findAll(pageable)
                .map(AbonnementMapper::toResponse);
    }
    @Transactional(readOnly = true)
    public Page<AbonnementResponse> findFiltered(
            Long clientId,
            StatutAbonnement statut,
            Pageable pageable) {

        Page<Abonnement> result;

        if (clientId != null && statut != null) {
            result = abonnementRepository
                    .findByClientIdAndStatut(clientId, statut, pageable);

        } else if (clientId != null) {
            result = abonnementRepository.findByClientId(clientId, pageable);

        } else if (statut != null) {
            result = abonnementRepository.findByStatut(statut, pageable);

        } else {
            result = abonnementRepository.findAll(pageable);
        }

        return result.map(AbonnementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AbonnementStatisticsResponse getStatistics() {

        long total = abonnementRepository.count();

        long actifs = abonnementRepository.countByStatut(StatutAbonnement.ACTIF);

        long suspendus = abonnementRepository.countByStatut(StatutAbonnement.SUSPENDU);

        long resilies = abonnementRepository.countByStatut(StatutAbonnement.RESILIE);

        long expires = abonnementRepository.countByStatut(StatutAbonnement.EXPIRE);

        return new AbonnementStatisticsResponse(
                total, actifs, suspendus, resilies, expires
        );
    }
    @Transactional(readOnly = true)
    public List<AbonnementResponse> getClientHistory(Long clientId) {

        return abonnementRepository
                .findByClientIdOrderByDateDebutDesc(clientId)
                .stream()
                .map(AbonnementMapper::toResponse)
                .toList();
    }

    public AbonnementResponse renew(Long id) {

        Abonnement oldAbonnement = getAbonnementOrThrow(id);

        if (oldAbonnement.getStatut() != StatutAbonnement.EXPIRE &&
                oldAbonnement.getStatut() != StatutAbonnement.RESILIE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only expired or cancelled subscriptions can be renewed"
            );
        }

        if (!Boolean.TRUE.equals(oldAbonnement.getPlan().getActif())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot renew with an inactive plan"
            );
        }

        Abonnement newAbonnement = new Abonnement();

        newAbonnement.setClientId(oldAbonnement.getClientId());
        newAbonnement.setPlan(oldAbonnement.getPlan());
        newAbonnement.setDateDebut(LocalDate.now());
        newAbonnement.setDateFin(null);
        newAbonnement.setProchaineDateFacturation(LocalDate.now().plusMonths(1));
        newAbonnement.setStatut(StatutAbonnement.ACTIF);

        Abonnement saved = abonnementRepository.save(newAbonnement);

        return AbonnementMapper.toResponse(saved);
    }
    @Transactional(readOnly = true)
    public Page<AbonnementResponse> searchSubscriptions(
            Long clientId,
            StatutAbonnement statut,
            String nomPlan,
            Pageable pageable) {

        String planFilter = (nomPlan == null) ? "" : nomPlan.trim();

        return abonnementRepository.searchSubscriptions(
                clientId,
                statut,
                planFilter,
                pageable
        ).map(AbonnementMapper::toResponse);
    }

    public AbonnementResponse update(
            Long id,
            AbonnementUpdateRequest request) {

        Abonnement abonnement = getAbonnementOrThrow(id);

        if (abonnement.getStatut() != StatutAbonnement.ACTIF &&
                abonnement.getStatut() != StatutAbonnement.SUSPENDU) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only active or suspended subscriptions can be updated"
            );
        }

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Plan not found"));

        if (!Boolean.TRUE.equals(plan.getActif())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Plan is inactive");
        }

        if (request.getDateFin() != null &&
                request.getDateFin().isBefore(request.getDateDebut())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End date cannot be before start date");
        }

        abonnement.setPlan(plan);
        abonnement.setDateDebut(request.getDateDebut());
        abonnement.setDateFin(request.getDateFin());
        abonnement.setExpirationNotificationSent(false);
        return AbonnementMapper.toResponse(
                abonnementRepository.save(abonnement));
    }


    @Transactional(readOnly = true)
    public void validateQuota(Long id, Integer nombreUtilisateurs) {

        Abonnement abonnement = getAbonnementOrThrow(id);

        Integer quota = abonnement.getPlan().getQuotaUtilisateurs();

        if (quota == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected plan has no user quota configured"
            );
        }

        if (nombreUtilisateurs > quota) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User limit exceeded. Maximum allowed: " + quota
            );
        }
    }

    @Transactional(readOnly = true)
    public List<AbonnementNotificationResponse> getUpcomingExpirations() {

        LocalDate today = LocalDate.now();
        LocalDate limitDate = today.plusDays(7);

        return abonnementRepository
                .findByStatutAndDateFinBetween(
                        StatutAbonnement.ACTIF,
                        today,
                        limitDate
                )
                .stream()
                .map(abonnement -> new AbonnementNotificationResponse(
                        abonnement.getId(),
                        abonnement.getClientId(),
                        abonnement.getPlan().getNom(),
                        abonnement.getDateFin(),
                        ChronoUnit.DAYS.between(today, abonnement.getDateFin())
                ))
                .toList();
    }


    public int sendExpirationNotifications() {

        LocalDate today = LocalDate.now();

        List<Abonnement> subscriptions =
                abonnementRepository.findByStatutAndDateFinBetween(
                        StatutAbonnement.ACTIF,
                        today,
                        today.plusDays(7)
                );

        int sent = 0;

        for (Abonnement abonnement : subscriptions) {

            // Skip subscriptions already notified
            if (abonnement.isExpirationNotificationSent()) {
                continue;
            }

            try {
                ClientDto client = clientServiceGateway.getClientById(
                        abonnement.getClientId()
                );

                if (client == null
                        || client.getEmail() == null
                        || client.getEmail().isBlank()) {
                    continue;
                }

                long daysRemaining = ChronoUnit.DAYS.between(
                        today,
                        abonnement.getDateFin()
                );

                abonnementEmailService.sendExpirationReminder(
                        client.getEmail(),
                        client.getNom(),
                        abonnement.getPlan().getNom(),
                        daysRemaining
                );

                // Mark as notified only after successful email delivery
                abonnement.setExpirationNotificationSent(true);
                abonnementRepository.save(abonnement);

                sent++;

            } catch (Exception e) {
                System.err.println(
                        "Failed to send notification for subscription "
                                + abonnement.getId() + ": " + e.getMessage()
                );
            }
        }

        return sent;
    }
    @Transactional(readOnly = true)
    public AbonnementDetailsResponse getAbonnementDetails(Long id) {

        // Retrieve subscription
        Abonnement abonnement = getAbonnementOrThrow(id);

        // Retrieve associated plan
        Plan plan = abonnement.getPlan();

        // Retrieve client from service-clients through Feign
        ClientDto client = clientServiceGateway.getClientById(
                abonnement.getClientId()
        );

        if (client == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Client not found"
            );
        }

        // Build plan DTO
        PlanDetailsResponse planDetails = new PlanDetailsResponse(
                plan.getId(),
                plan.getNom(),
                plan.getPrixMensuel(),
                plan.getQuotaUtilisateurs(),
                plan.getActif()
        );

        // Build composed response
        return new AbonnementDetailsResponse(
                AbonnementMapper.toResponse(abonnement),
                planDetails,
                client
        );
    }
    public List<ClientDto> getAllClients() {
        return clientServiceGateway.getAllClients();
    }




}
