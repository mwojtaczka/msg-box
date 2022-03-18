package com.maciej.wojtaczka.messagebox.messaging;

import com.maciej.wojtaczka.messagebox.domain.PostMan;
import com.maciej.wojtaczka.messagebox.domain.model.Envelope;
import com.maciej.wojtaczka.messagebox.domain.model.Message;
import com.maciej.wojtaczka.messagebox.domain.model.MessageStatusUpdated;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class KafkaPostMan implements PostMan {

	public static final String MESSAGE_ACCEPTED_TOPIC = "message-accepted";
	public static final String MESSAGE_STATUS_UPDATED = "message-status-updated";
	private final ReactiveKafkaProducerTemplate<String, Envelope<Message>> kafkaMessagesProducerTemplate;
	private final ReactiveKafkaProducerTemplate<String, Envelope<MessageStatusUpdated>> kafkaMessagesStatusProducerTemplate;

	public KafkaPostMan(ReactiveKafkaProducerTemplate<String, Envelope<Message>> kafkaProducerTemplate,
						ReactiveKafkaProducerTemplate<String, Envelope<MessageStatusUpdated>> kafkaMessagesStatusProducerTemplate) {
		this.kafkaMessagesProducerTemplate = kafkaProducerTemplate;
		this.kafkaMessagesStatusProducerTemplate = kafkaMessagesStatusProducerTemplate;
	}

	@Override
	public Mono<Void> deliver(Envelope<Message> message) {
		return kafkaMessagesProducerTemplate.send(MESSAGE_ACCEPTED_TOPIC, message)
											.flatMap(result -> {
												if (result.exception() == null) {
													return Mono.empty();
												} else {
													return Mono.error(result::exception);
												}
											});
	}

	@Override
	public Mono<Void> notifyAboutMsgStatusUpdated(Envelope<MessageStatusUpdated> messageSeen) {
		return kafkaMessagesStatusProducerTemplate.send(MESSAGE_STATUS_UPDATED, messageSeen)
												  .flatMap(result -> {
													  if (result.exception() == null) {
														  return Mono.empty();
													  } else {
														  return Mono.error(result::exception);
													  }
												  });
	}
}
