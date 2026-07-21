package com.fairshare.settlement.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.settlement-events}")
    private String settlementEventsTopic;

    @Bean
    public NewTopic settlementEventsTopic() {
        // Same partition count as expense-events for consistency, though the
        // two topics don't need to match, ledger-service's per-group
        // ordering guarantee only depends on the key (groupId), not on
        // partition counts lining up across topics.
        return TopicBuilder.name(settlementEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
