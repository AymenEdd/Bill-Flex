
package org.gmp.serviceabonnements.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AbonnementEmailService {

    private final JavaMailSender mailSender;

    public AbonnementEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendExpirationReminder(
            String email,
            String clientName,
            String planName,
            long daysRemaining) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("Subscription expiration reminder");

        message.setText(
                "Hello " + clientName + ",\n\n" +
                        "Your " + planName + " subscription will expire in " +
                        daysRemaining + " days.\n\n" +
                        "Please renew your subscription.\n\n" +
                        "Best regards,\nBill-Flex Team"
        );

        mailSender.send(message);
    }
}
