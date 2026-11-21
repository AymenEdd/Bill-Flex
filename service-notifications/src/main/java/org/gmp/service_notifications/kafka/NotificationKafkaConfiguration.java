package org.gmp.service_notifications.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
class NotificationKafkaConfiguration {

    @Bean
    DefaultErrorHandler notificationKafkaErrors(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer deadLetter = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, error) -> new TopicPartition(record.topic() + ".DLT", -1));
        deadLetter.setFailIfSendResultIsError(true);
        DefaultErrorHandler handler = new DefaultErrorHandler(deadLetter, new FixedBackOff(1000L, 2L));
        handler.addNotRetryableExceptions(InvalidNotificationEventException.class);
        return handler;
    }
}
