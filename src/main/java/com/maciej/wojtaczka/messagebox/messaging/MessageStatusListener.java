package com.maciej.wojtaczka.messagebox.messaging;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import com.maciej.wojtaczka.messagebox.domain.model.MessageSeen;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;

public class MessageStatusListener {

	private final ReactiveKafkaConsumerTemplate<String, MessageSeen> kafkaMessageListener;
	private final ConversationService messageService;

	public MessageStatusListener(ReactiveKafkaConsumerTemplate<String, MessageSeen> kafkaMessageListener,
						   ConversationService messageService) {
		this.kafkaMessageListener = kafkaMessageListener;
		this.messageService = messageService;
	}

	void listen() {
		kafkaMessageListener
				.receive()
				.map(ConsumerRecord::value)
				.flatMap(messageService::updateMessageStatus)
				.subscribe();
	}
}
