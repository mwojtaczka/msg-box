package com.maciej.wojtaczka.messagebox.messaging;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import com.maciej.wojtaczka.messagebox.domain.model.UserConnection;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;

public class ConnectionListener {

	private final ReactiveKafkaConsumerTemplate<String, UserConnection> kafkaConnectionListener;
	private final ConversationService conversationService;

	public ConnectionListener(ReactiveKafkaConsumerTemplate<String, UserConnection> kafkaConnectionListener,
							  ConversationService messageService) {
		this.kafkaConnectionListener = kafkaConnectionListener;
		this.conversationService = messageService;
	}

	void listen() {
		kafkaConnectionListener.receive()
							   .map(ConsumerRecord::value)
							   .flatMap(conversationService::createFaceToFaceConversation)
							   .subscribe();
	}
}
