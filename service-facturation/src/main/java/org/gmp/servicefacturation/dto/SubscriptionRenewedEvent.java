package org.gmp.servicefacturation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRenewedEvent {

    private String eventId;
    private Long subscriptionId;
    private Long clientId;
    private Long planId;
    private String periode;
}
