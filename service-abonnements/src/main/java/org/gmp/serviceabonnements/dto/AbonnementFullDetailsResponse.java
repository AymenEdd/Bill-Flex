package org.gmp.serviceabonnements.dto;

import org.gmp.serviceabonnements.client.ClientDto;
import org.gmp.serviceabonnements.client.InvoiceDto;

import java.util.List;

public class AbonnementFullDetailsResponse {

    private AbonnementResponse abonnement;
    private PlanDetailsResponse plan;
    private ClientDto client;
    private List<InvoiceDto> invoices;

    public AbonnementFullDetailsResponse() {
    }

    public AbonnementFullDetailsResponse(
            AbonnementResponse abonnement,
            PlanDetailsResponse plan,
            ClientDto client,
            List<InvoiceDto> invoices) {
        this.abonnement = abonnement;
        this.plan = plan;
        this.client = client;
        this.invoices = invoices;
    }

    public AbonnementResponse getAbonnement() {
        return abonnement;
    }

    public PlanDetailsResponse getPlan() {
        return plan;
    }

    public ClientDto getClient() {
        return client;
    }

    public List<InvoiceDto> getInvoices() {
        return invoices;
    }
}