package com.maciej.wojtaczka.messagebox.messaging;

import com.maciej.wojtaczka.messagebox.domain.ConversationStorage;
import com.maciej.wojtaczka.messagebox.domain.MessageService;
import com.maciej.wojtaczka.messagebox.domain.PostMan;
import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

import java.util.Collections;
import java.util.Map;

@Configuration
public class MessagingConfiguration {

	public static final String MESSAGE_RECEIVED_TOPIC = "message-received";

	@Value("${spring.application.name}")
	private String applicationName;

	@Bean
	ReceiverOptions<String, Message> kafkaReceiverOptions(KafkaProperties kafkaProperties) {
		ReceiverOptions<String, Message> basicReceiverOptions = ReceiverOptions.create(kafkaProperties.buildConsumerProperties());
		return basicReceiverOptions.subscription(Collections.singletonList(MESSAGE_RECEIVED_TOPIC))
								   .consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, applicationName)
								   .consumerProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
	}

	@Bean
	ReactiveKafkaConsumerTemplate<String, Message> reactiveKafkaConsumerTemplate(
			ReceiverOptions<String, Message> kafkaReceiverOptions) {
		return new ReactiveKafkaConsumerTemplate<>(kafkaReceiverOptions);
	}

	@Bean
	ReactiveKafkaProducerTemplate<String, Envelope> reactiveKafkaProducerTemplate(
			KafkaProperties properties) {
		Map<String, Object> props = properties
				.buildProducerProperties();

		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

		return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(props));
	}

	@Bean
	MessageListener messageListener(ReactiveKafkaConsumerTemplate<String, Message> consumerTemplate,
									ConversationStorage conversationStorage,
									PostMan postMan) {
		MessageService messageService = new MessageService(conversationStorage, postMan);
		MessageListener messageListener = new MessageListener(consumerTemplate, messageService);
		messageListener.listen();

		return messageListener;
	}
}
