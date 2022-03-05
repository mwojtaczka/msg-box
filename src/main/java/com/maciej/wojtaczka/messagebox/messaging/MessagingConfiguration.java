package com.maciej.wojtaczka.messagebox.messaging;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import com.maciej.wojtaczka.messagebox.domain.ConversationStorage;
import com.maciej.wojtaczka.messagebox.domain.PostMan;
import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import com.maciej.wojtaczka.messagebox.domain.model.MessageSeen;
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
	public static final String MESSAGE_SEEN_TOPIC = "message-seen";

	@Value("${spring.application.name}")
	private String applicationName;

	@Bean
	ReactiveKafkaConsumerTemplate<String, Message> reactiveMessageConsumerTemplate(KafkaProperties kafkaProperties) {
		ReceiverOptions<String, Message> basicReceiverOptions = ReceiverOptions.create(kafkaProperties.buildConsumerProperties());
		ReceiverOptions<String, Message> messageReceiverOptions =
				basicReceiverOptions.subscription(Set.of(MESSAGE_RECEIVED_TOPIC))
									.consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, applicationName)
									.consumerProperty(JsonDeserializer.VALUE_DEFAULT_TYPE, Message.class)
									.consumerProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

		return new ReactiveKafkaConsumerTemplate<>(messageReceiverOptions);
	}

	@Bean
	ReactiveKafkaConsumerTemplate<String, UserConnection> reactiveConnectionConsumerTemplate(KafkaProperties kafkaProperties) {
		ReceiverOptions<String, UserConnection> basicReceiverOptions = ReceiverOptions.create(kafkaProperties.buildConsumerProperties());
		ReceiverOptions<String, UserConnection> messageReceiverOptions =
				basicReceiverOptions.subscription(Set.of(CONNECTION_CREATED_TOPIC))
									.consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, applicationName)
									.consumerProperty(JsonDeserializer.VALUE_DEFAULT_TYPE, UserConnection.class)
									.consumerProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

		return new ReactiveKafkaConsumerTemplate<>(messageReceiverOptions);
	}

	@Bean
	ReactiveKafkaConsumerTemplate<String, MessageSeen> reactiveMessageStatusConsumerTemplate(KafkaProperties kafkaProperties) {
		ReceiverOptions<String, MessageSeen> basicReceiverOptions = ReceiverOptions.create(kafkaProperties.buildConsumerProperties());
		ReceiverOptions<String, MessageSeen> messageReceiverOptions =
				basicReceiverOptions.subscription(Set.of(MESSAGE_SEEN_TOPIC))
									.consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, applicationName)
									.consumerProperty(JsonDeserializer.VALUE_DEFAULT_TYPE, MessageSeen.class)
									.consumerProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

		return new ReactiveKafkaConsumerTemplate<>(messageReceiverOptions);
	}

	@Bean
	ReactiveKafkaProducerTemplate<String, Envelope<Message>> reactiveKafkaMessagesProducerTemplate(
			KafkaProperties properties) {

		Map<String, Object> props = properties
				.buildProducerProperties();

		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

		return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(props));
	}

	@Bean
	ReactiveKafkaProducerTemplate<String, Envelope<MessageSeen>> reactiveKafkaMessagesStatusProducerTemplate(
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
		var messageListener = new MessageListener(reactiveMessageConsumerTemplate, conversationService);
		messageListener.listen();

		return messageListener;
	}

	@Bean
	ConnectionListener connectionListener(ReactiveKafkaConsumerTemplate<String, UserConnection> reactiveConnectionConsumerTemplate,
										  ConversationService conversationService) {
		var connectionListener = new ConnectionListener(reactiveConnectionConsumerTemplate, conversationService);
		connectionListener.listen();

		return connectionListener;
	}

	@Bean
	MessageStatusListener messageStatusListener(ReactiveKafkaConsumerTemplate<String, MessageSeen> reactiveMessageStatusConsumerTemplate,
												ConversationService conversationService) {
		var messageListener = new MessageStatusListener(reactiveMessageStatusConsumerTemplate, conversationService);
		messageListener.listen();

		return messageListener;
	}


}
