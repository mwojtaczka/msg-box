package com.maciej.wojtaczka.messagebox.messaging;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;

public class MessageListener {

	private final ReactiveKafkaConsumerTemplate<String, Message> kafkaMessageListener;
	private final ConversationService messageService;

	public MessageListener(ReactiveKafkaConsumerTemplate<String, Message> kafkaMessageListener,
						   ConversationService messageService) {
		this.kafkaMessageListener = kafkaMessageListener;
		this.messageService = messageService;
	}

	void listen() {
		kafkaMessageListener
				.receive()
				.map(ConsumerRecord::value)
				.flatMap(messageService::acceptMessage)
				.subscribe();
	}
}
