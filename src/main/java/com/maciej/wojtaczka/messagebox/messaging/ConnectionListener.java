package com.maciej.wojtaczka.messagebox.messaging;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import com.maciej.wojtaczka.messagebox.domain.model.UserConnection;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;

public class ConnectionListener {

	private final ReactiveKafkaConsumerTemplate<String, Envelope<UserConnection>> kafkaConnectionListener;
	private final ConversationService conversationService;

	public ConnectionListener(ReactiveKafkaConsumerTemplate<String, Envelope<UserConnection>> kafkaConnectionListener,
							  ConversationService messageService) {
		this.kafkaConnectionListener = kafkaConnectionListener;
		this.conversationService = messageService;
	}

	void listen() {
		kafkaConnectionListener.receive()
							   .map(stringEnvelopeReceiverRecord -> stringEnvelopeReceiverRecord.value().getPayload())
							   .flatMap(conversationService::createFaceToFaceConversation)
							   .subscribe();
	}
}
