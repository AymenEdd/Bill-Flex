package org.gmp.servicefacturation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gmp.servicefacturation.dto.*;
import org.gmp.servicefacturation.entity.StatutFacture;
import org.gmp.servicefacturation.scheduler.InvoiceScheduler;
import org.gmp.servicefacturation.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/invoices", "/api/invoices"})
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceScheduler invoiceScheduler;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@Valid @RequestBody InvoiceRequest request) {
        return invoiceService.createInvoice(request);
    }

    @GetMapping
    public List<InvoiceResponse> getAllInvoices(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long subscriptionId,
            @RequestParam(required = false) StatutFacture statut
    ) {
        return invoiceService.getAllInvoices(clientId, subscriptionId, statut);
    }

    @GetMapping("/{id}")
    public InvoiceResponse getInvoiceById(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id);
    }

    @GetMapping("/{id}/details")
    public InvoiceEnrichedResponse getInvoiceDetails(@PathVariable Long id) {
        return invoiceService.getInvoiceEnrichedDetails(id);
    }

    @PutMapping("/{id}")
    public InvoiceResponse updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceRequest request
    ) {
        return invoiceService.updateInvoice(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
    }

    @GetMapping("/{id}/lines")
    public List<InvoiceLineResponse> getInvoiceLines(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id).getLines();
    }

    @PostMapping("/{id}/lines")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse addLine(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceLineRequest lineRequest
    ) {
        return invoiceService.addLineToInvoice(id, lineRequest);
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    public InvoiceResponse removeLine(
            @PathVariable Long id,
            @PathVariable Long lineId
    ) {
        return invoiceService.removeLineFromInvoice(id, lineId);
    }

    @PostMapping("/{id}/emit")
    public InvoiceResponse emitInvoice(@PathVariable Long id) {
        return invoiceService.emitInvoice(id);
    }

    @PostMapping("/{id}/pay")
    public InvoiceResponse payInvoice(@PathVariable Long id) {
        return invoiceService.markAsPaid(id);
    }

    @PostMapping("/{id}/cancel")
    public InvoiceResponse cancelInvoice(@PathVariable Long id) {
        return invoiceService.cancelInvoice(id);
    }

    @PostMapping("/check-overdue")
    public ResponseEntity<Map<String, Object>> triggerOverdueCheck() {
        int updated = invoiceScheduler.checkAndMarkOverdueInvoices();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Vérification des factures échues exécutée",
                "updatedCount", updated
        ));
    }
}
