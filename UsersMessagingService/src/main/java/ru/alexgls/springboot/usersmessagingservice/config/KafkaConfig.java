package ru.alexgls.springboot.usersmessagingservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import ru.alexgls.springboot.usersmessagingservice.dto.CreateMessagePayload;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;
import ru.alexgls.springboot.usersmessagingservice.dto.CreatedMessageDto;


import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic chatTopic() {
        return TopicBuilder
                .name("chat-topic")
                .build();
    }

    @Bean
    public NewTopic messagesStorageTopic() {
        return TopicBuilder
                .name("messaging-topic")
                .build();
    }

    @Bean
    public ProducerFactory<String, CreateMessagePayload> producerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        JsonSerializer<CreateMessagePayload> jsonSerializer = new JsonSerializer<>();
        return new DefaultKafkaProducerFactory<>(properties, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, CreateMessagePayload> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, CreatedMessageDto> messageConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        JsonDeserializer<CreatedMessageDto> jsonDeserializer = new JsonDeserializer<>(CreatedMessageDto.class);
        jsonDeserializer.addTrustedPackages("/**");
        jsonDeserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CreatedMessageDto> kafkaMessageListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CreatedMessageDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(messageConsumerFactory());
        return factory;
    }
}
