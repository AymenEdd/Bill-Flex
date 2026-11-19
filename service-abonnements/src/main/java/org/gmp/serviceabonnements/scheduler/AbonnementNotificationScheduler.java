
package org.gmp.serviceabonnements.scheduler;

import org.gmp.serviceabonnements.service.AbonnementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AbonnementNotificationScheduler {

    private final AbonnementService abonnementService;

    public AbonnementNotificationScheduler(
            AbonnementService abonnementService) {
        this.abonnementService = abonnementService;
    }

    // Automatically expire subscriptions every day at 00:05
    @Scheduled(cron = "0 5 0 * * *", zone = "Africa/Casablanca")
    public void expireSubscriptionsAutomatically() {

        int expired = abonnementService.expireSubscriptions();

        System.out.println(
                "Automatic expiration: "
                        + expired + " subscription(s) expired."
        );
    }

    // Automatically send expiration reminder emails every day at 09:00
    @Scheduled(cron = "0 0 9 * * *", zone = "Africa/Casablanca")
    public void sendExpirationNotificationsAutomatically() {

        int sent = abonnementService.sendExpirationNotifications();

        System.out.println(
                "Automatic expiration notifications: "
                        + sent + " email(s) sent."
        );
    }
}
