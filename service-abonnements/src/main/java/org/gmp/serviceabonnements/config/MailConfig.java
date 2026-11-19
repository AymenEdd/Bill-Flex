
package org.gmp.serviceabonnements.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender(Environment env) {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(
                env.getProperty("spring.mail.host", "localhost")
        );

        mailSender.setPort(
                env.getProperty("spring.mail.port", Integer.class, 1025)
        );

        String username = env.getProperty("spring.mail.username");
        String password = env.getProperty("spring.mail.password");

        if (username != null && !username.isBlank()) {
            mailSender.setUsername(username);
        }

        if (password != null && !password.isBlank()) {
            mailSender.setPassword(password);
        }

        Properties properties = mailSender.getJavaMailProperties();

        properties.put("mail.smtp.auth", "false");
        properties.put("mail.smtp.starttls.enable", "false");

        return mailSender;
    }
}
