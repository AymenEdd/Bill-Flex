package org.gmp.service_notifications.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.gmp.service_notifications.dto.CreateNotificationRequest;
import org.gmp.service_notifications.dto.NotificationResponse;
import org.gmp.service_notifications.exception.NotificationConflictException;
import org.gmp.service_notifications.mapper.NotificationMapper;
import org.gmp.service_notifications.service.NotificationDelivery;
import org.gmp.service_notifications.service.NotificationStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationStore store;
    private final NotificationDelivery delivery;

    public NotificationController(NotificationStore store, NotificationDelivery delivery) {
        this.store = store;
        this.delivery = delivery;
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestParam(required = false) @Positive Long clientId,
                                           @RequestParam(required = false) @Positive Long invoiceId) {
        return store.list(clientId, invoiceId).stream().map(NotificationMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable @Positive Long id) {
        return NotificationMapper.toResponse(store.find(id));
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
        NotificationStore.Creation creation = store.create(request.clientId(), request.invoiceId(), request.type());
        if (!creation.created()) {
            throw new NotificationConflictException();
        }
        Long id = creation.notification().getId();
        delivery.deliver(id);
        return ResponseEntity.created(URI.create("/notifications/" + id))
                .body(NotificationMapper.toResponse(store.find(id)));
    }
}
