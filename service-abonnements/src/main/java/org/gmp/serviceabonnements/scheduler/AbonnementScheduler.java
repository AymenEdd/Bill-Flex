
package org.gmp.serviceabonnements.scheduler;

import org.gmp.serviceabonnements.service.AbonnementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AbonnementScheduler {

    private final AbonnementService abonnementService;

    public AbonnementScheduler(AbonnementService abonnementService) {
        this.abonnementService = abonnementService;
    }

    // Execute every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void expireSubscriptions() {

        int count = abonnementService.expireSubscriptions();

        System.out.println(
                "Subscription expiration completed. Expired: " + count
        );
    }
    @Scheduled(cron = "0 5 0 * * *")
    public void processBillingDates() {

        int count = abonnementService.processDueBillingDates();

        System.out.println(
                "Billing date processing completed. Processed: " + count
        );
    }
    @Scheduled(cron = "0 0 9 * * *", zone = "Africa/Casablanca")
    public void sendExpirationNotifications() {
        int sent = abonnementService.sendExpirationNotifications();

        System.out.println(
                "Automatic expiration notifications: "
                        + sent + " email(s) sent."
        );
    }
}
