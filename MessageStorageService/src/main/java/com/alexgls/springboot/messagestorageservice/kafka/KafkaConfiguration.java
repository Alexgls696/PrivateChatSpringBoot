package com.alexgls.springboot.messagestorageservice.kafka;

import com.alexgls.springboot.messagestorageservice.dto.CreateMessagePayload;
import com.alexgls.springboot.messagestorageservice.dto.CreatedMessageDto;
import com.alexgls.springboot.messagestorageservice.dto.UpdateMessagePayload;
import com.alexgls.springboot.messagestorageservice.entity.Message;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfiguration {

    private Map<String, Object> initializeProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "messaging-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    @Bean
    public ConsumerFactory<String, CreateMessagePayload> createMessageConsumerFactory() {
        Map<String, Object> props = initializeProperties();
        JsonDeserializer<CreateMessagePayload> jsonDeserializer = new JsonDeserializer<>(CreateMessagePayload.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CreateMessagePayload> kafkaCreateMessageListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CreateMessagePayload> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(createMessageConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, UpdateMessagePayload> updateMessageConsumerFactory() {
        Map<String, Object> props = initializeProperties();
        JsonDeserializer<UpdateMessagePayload> jsonDeserializer = new JsonDeserializer<>(UpdateMessagePayload.class);
        jsonDeserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UpdateMessagePayload> kafkaUpdateMessageListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UpdateMessagePayload> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(updateMessageConsumerFactory());
        return factory;
    }

    @Bean
    public ProducerFactory<String, CreatedMessageDto> messageProducerFactory() {
        Map<String, Object> props = initializeProperties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        JsonSerializer<CreatedMessageDto> jsonSerializer = new JsonSerializer<>();
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), jsonSerializer);
    }

    @Bean
    public KafkaTemplate<String, CreatedMessageDto> kafkaTemplate() {
        return new KafkaTemplate<>(messageProducerFactory());
    }

    @Bean
    public NewTopic eventsTopic() {
        return TopicBuilder
                .name("events-message-created")
                .build();
    }

}
