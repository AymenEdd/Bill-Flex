
package org.gmp.serviceabonnements.dto;

import org.gmp.serviceabonnements.client.ClientDto;

public class AbonnementDetailsResponse {

    private AbonnementResponse abonnement;
    private PlanDetailsResponse plan;
    private ClientDto client;

    public AbonnementDetailsResponse() {
    }

    public AbonnementDetailsResponse(
            AbonnementResponse abonnement,
            PlanDetailsResponse plan,
            ClientDto client) {
        this.abonnement = abonnement;
        this.plan = plan;
        this.client = client;
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
}
