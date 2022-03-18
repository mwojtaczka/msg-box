package com.maciej.wojtaczka.messagebox.messaging;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import com.maciej.wojtaczka.messagebox.domain.model.MessageStatusUpdated;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;

@Slf4j
public class MessageStatusListener {

	private final ReactiveKafkaConsumerTemplate<String, MessageStatusUpdated> kafkaMessageListener;
	private final ConversationService messageService;

	public MessageStatusListener(ReactiveKafkaConsumerTemplate<String, MessageStatusUpdated> kafkaMessageListener,
						   ConversationService messageService) {
		this.kafkaMessageListener = kafkaMessageListener;
		this.messageService = messageService;
	}

	void listen() {
		kafkaMessageListener
				.receive()
				.doOnNext(r -> log.info("Received status changed event: {}", r))
				.map(ConsumerRecord::value)
				.flatMap(messageService::updateMessageStatus)
				.onErrorContinue((throwable, o) -> log.error("Error while processing message status changed event: {}", throwable.getMessage()))
				.subscribe();
	}
}
