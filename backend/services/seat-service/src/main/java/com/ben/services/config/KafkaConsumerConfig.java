package com.ben.services.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "seat-service-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ben.common_lib.event");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler);   // retry with backoff, then DLT (never drop)
        return factory;
    }

    // Producer used only to publish failed messages to <topic>.DLT (seat-service sends nothing else)
    @Bean
    public KafkaTemplate<String, Object> dltKafkaTemplate() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config));
    }

    /**
     * A failing message (dependency down, bug) is retried with growing waits: 1 s, 2 s, 4 s ... capped at 1 min,
     * 10 retries ≈ 5 min, then published to <topic>.DLT instead of being dropped, and the partition moves on.
     * The offset is committed only after success, so a crash mid-processing redelivers the message (consumers
     * are idempotent). Messages that can never be read (bad JSON) skip the retries and go straight to the DLT.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> dltTemplate) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(10);
        backOff.setInitialInterval(1_000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(60_000);
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(dltTemplate), backOff);
    }

}
