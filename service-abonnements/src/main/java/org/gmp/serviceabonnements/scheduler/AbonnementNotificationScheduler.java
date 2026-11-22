
package org.gmp.serviceabonnements.scheduler;

import org.gmp.serviceabonnements.service.AbonnementService;

/**
 * Delegated to {@link AbonnementScheduler} to prevent duplicate scheduled execution.
 */
public class AbonnementNotificationScheduler {

    private final AbonnementService abonnementService;

    public AbonnementNotificationScheduler(
            AbonnementService abonnementService) {
        this.abonnementService = abonnementService;
    }

    public void expireSubscriptionsAutomatically() {
        abonnementService.expireSubscriptions();
    }

    public void sendExpirationNotificationsAutomatically() {
        abonnementService.sendExpirationNotifications();
    }
}
