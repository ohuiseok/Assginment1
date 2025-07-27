package com.coupon.assignment.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "kafka.auto.start", havingValue = "true", matchIfMissing = false)
public class KafkaConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);
    
    @Bean
    public NewTopic couponProcessingTopic() {
        return TopicBuilder.name("coupon-processing")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "86400000") // 1일 보관
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Kafka initialized!");
    }
}
