package com.maciej.wojtaczka.messagebox.config;

import com.maciej.wojtaczka.messagebox.domain.model.Message;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class TestConfig {

	@Bean
	KafkaTemplate<String, Message> kafkaTemplate(
			KafkaProperties properties) {
		DefaultKafkaProducerFactory<String, Message> producerFactory =
				new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(), new StringSerializer(), new JsonSerializer<>());

		return new KafkaTemplate<>(producerFactory);
	}
}
