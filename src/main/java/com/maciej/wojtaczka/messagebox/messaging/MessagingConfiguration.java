package com.maciej.wojtaczka.messagebox.messaging;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import com.maciej.wojtaczka.messagebox.domain.ConversationStorage;
import com.maciej.wojtaczka.messagebox.domain.PostMan;
import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import com.maciej.wojtaczka.messagebox.domain.model.UserConnection;
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

import java.util.Map;
import java.util.Set;

@Configuration
public class MessagingConfiguration {

	public static final String MESSAGE_RECEIVED_TOPIC = "message-received";
	public static final String CONNECTION_CREATED_TOPIC = "connection-created";

	@Value("${spring.application.name}")
	private String applicationName;

	@Bean
	ReactiveKafkaConsumerTemplate<String, Message> reactiveMessageConsumerTemplate(KafkaProperties kafkaProperties) {
		ReceiverOptions<String, Message> basicReceiverOptions = ReceiverOptions.create(kafkaProperties.buildConsumerProperties());
		ReceiverOptions<String, Message> messageReceiverOptions =
				basicReceiverOptions.subscription(Set.of(MESSAGE_RECEIVED_TOPIC))
									.consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, applicationName)
									.consumerProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

		return new ReactiveKafkaConsumerTemplate<>(messageReceiverOptions);
	}

	@Bean
	ReactiveKafkaConsumerTemplate<String, UserConnection> reactiveConnectionConsumerTemplate(KafkaProperties kafkaProperties) {
		ReceiverOptions<String, UserConnection> basicReceiverOptions = ReceiverOptions.create(kafkaProperties.buildConsumerProperties());
		ReceiverOptions<String, UserConnection> messageReceiverOptions =
				basicReceiverOptions.subscription(Set.of(CONNECTION_CREATED_TOPIC))
									.consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, applicationName)
									.consumerProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

		return new ReactiveKafkaConsumerTemplate<>(messageReceiverOptions);
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
	ConversationService conversationService(ConversationStorage conversationStorage,
											PostMan postMan) {
		return new ConversationService(conversationStorage, postMan);
	}

	@Bean
	MessageListener messageListener(ReactiveKafkaConsumerTemplate<String, Message> reactiveMessageConsumerTemplate,
									ConversationService conversationService) {
		MessageListener messageListener = new MessageListener(reactiveMessageConsumerTemplate, conversationService);
		messageListener.listen();

		return messageListener;
	}

	@Bean
	ConnectionListener connectionListener(ReactiveKafkaConsumerTemplate<String, UserConnection> reactiveConnectionConsumerTemplate,
										  ConversationService conversationService) {
		ConnectionListener connectionListener = new ConnectionListener(reactiveConnectionConsumerTemplate, conversationService);
		connectionListener.listen();

		return connectionListener;
	}


}
