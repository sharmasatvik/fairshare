package com.fairshare.expense.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.expense-events}")
    private String expenseEventsTopic;

    /**
     * 3 partitions: enough to demonstrate that events for *different* groups
     * can be processed in parallel by the ledger-service consumer group,
     * while events for the *same* group (same partition key) still land on
     * one partition and stay ordered. Auto-created here for local dev
     * convenience; a real deployment would provision topics out-of-band.
     */
    @Bean
    public NewTopic expenseEventsTopic() {
        return TopicBuilder.name(expenseEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
