package com.arsiwooqq.orderservice.config;

import com.arsiwooqq.orderservice.event.OrderCreatedEvent;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "kafka.producer")
@Getter
@Setter
public class KafkaProducerConfig {

    @Value("${kafka.producer.topics.order-created.name}")
    private String topicName;

    @Value("${kafka.producer.topics.order-created.partitions}")
    private int partitions;

    @Value("${kafka.producer.topics.order-created.replicas}")
    private int replicas;

    @Value("${kafka.producer.topics.order-created.min-insync}")
    private String minInsync;

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder
                .name(topicName)
                .partitions(partitions)
                .replicas(replicas)
                .configs(Map.of("min.insync.replicas", minInsync))
                .build();
    }

    @Bean
    public NewTopic orderCreatedDltTopic() {
        return TopicBuilder
                .name(topicName + "-dlt")
                .partitions(partitions)
                .replicas(replicas)
                .configs(Map.of("min.insync.replicas", minInsync))
                .build();
    }

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private String acks = "all";

    private String requestTimeout = "5000";

    private String enableIdempotence = "true";

    private String linger = "0";

    private String deliveryTimeout = "120000";

    @Bean
    public ProducerFactory<String, OrderCreatedEvent> orderCreatedEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerFactoryConfig());
    }

    @Bean
    public KafkaTemplate<String, OrderCreatedEvent> orderCreatedEventKafkaTemplate(
            ProducerFactory<String, OrderCreatedEvent> factory) {
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerFactoryConfig());
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    private Map<String, Object> producerFactoryConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, acks);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeout);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);
        config.put(ProducerConfig.LINGER_MS_CONFIG, linger);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeout);

        return config;
    }
}
