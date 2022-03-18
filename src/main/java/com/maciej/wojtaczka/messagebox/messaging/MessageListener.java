package com.maciej.wojtaczka.messagebox.messaging;

import com.maciej.wojtaczka.messagebox.domain.ConversationService;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;

@Slf4j
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
				.doOnNext(r -> log.debug("Received message-received event: " + r))
				.map(ConsumerRecord::value)
				.flatMap(messageService::acceptMessage)
				.onErrorContinue((throwable, o) -> log.error("Error while processing message received event: " + throwable.getMessage()))
				.subscribe();
	}
}
