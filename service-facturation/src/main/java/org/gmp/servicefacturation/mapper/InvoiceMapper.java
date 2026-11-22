package org.gmp.servicefacturation.mapper;

import org.gmp.servicefacturation.dto.InvoiceLineRequest;
import org.gmp.servicefacturation.dto.InvoiceLineResponse;
import org.gmp.servicefacturation.dto.InvoiceRequest;
import org.gmp.servicefacturation.dto.InvoiceResponse;
import org.gmp.servicefacturation.entity.Invoice;
import org.gmp.servicefacturation.entity.InvoiceLine;
import org.gmp.servicefacturation.entity.StatutFacture;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class InvoiceMapper {

    public Invoice toEntity(InvoiceRequest request) {
        if (request == null) {
            return null;
        }

        Invoice invoice = Invoice.builder()
                .subscriptionId(request.getSubscriptionId())
                .clientId(request.getClientId())
                .periode(request.getPeriode())
                .echeance(request.getEcheance())
                .tauxTva(request.getTauxTva() != null ? request.getTauxTva() : new BigDecimal("20.00"))
                .statut(StatutFacture.BROUILLON)
                .lines(new ArrayList<>())
                .build();

        if (request.getLines() != null) {
            for (InvoiceLineRequest lineRequest : request.getLines()) {
                invoice.addLine(toEntity(lineRequest));
            }
        }

        invoice.recalculerMontants();
        return invoice;
    }

    public InvoiceLine toEntity(InvoiceLineRequest request) {
        if (request == null) {
            return null;
        }

        return InvoiceLine.builder()
                .libelle(request.getLibelle())
                .quantite(request.getQuantite())
                .prixUnitaire(request.getPrixUnitaire())
                .build();
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        List<InvoiceLineResponse> lineResponses = new ArrayList<>();
        if (invoice.getLines() != null) {
            for (InvoiceLine line : invoice.getLines()) {
                lineResponses.add(toResponse(line));
            }
        }

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .subscriptionId(invoice.getSubscriptionId())
                .clientId(invoice.getClientId())
                .periode(invoice.getPeriode())
                .montantHT(invoice.getMontantHT())
                .montantTTC(invoice.getMontantTTC())
                .tauxTva(invoice.getTauxTva())
                .echeance(invoice.getEcheance())
                .statut(invoice.getStatut())
                .lines(lineResponses)
                .build();
    }

    public InvoiceLineResponse toResponse(InvoiceLine line) {
        if (line == null) {
            return null;
        }

        return InvoiceLineResponse.builder()
                .id(line.getId())
                .invoiceId(line.getInvoice() != null ? line.getInvoice().getId() : null)
                .libelle(line.getLibelle())
                .quantite(line.getQuantite())
                .prixUnitaire(line.getPrixUnitaire())
                .totalLigne(line.calculerSousTotal())
                .build();
    }
}
